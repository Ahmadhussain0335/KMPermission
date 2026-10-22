package io.github.ahmadhussain0335.kmppermission.internal

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import io.github.ahmadhussain0335.kmppermission.Permission
import io.github.ahmadhussain0335.kmppermission.PermissionException
import io.github.ahmadhussain0335.kmppermission.PermissionStatus
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

/**
 * Real Android runtime engine, built on the (non-deprecated) Activity Result API.
 *
 * Bound to a live [ComponentActivity] via [attach] — see that function's KDoc for the lifecycle
 * contract. Every operation throws [PermissionException.ManagerNotAttached] if called while detached.
 */
internal actual class PermissionController actual constructor() : PermissionEngine {

    // Written on main, read from status() on any thread.
    @Volatile
    private var host: Host? = null

    private class Host(val activity: ComponentActivity, val launcher: ActivityResultLauncher<Array<String>>) {
        var pendingContinuation: CancellableContinuation<Unit>? = null
        val requestedStore = RequestedPermissionsStore(activity.applicationContext)
        val resolver = PermissionStatusResolver(activity.applicationContext, requestedStore)
    }

    /**
     * Binds this controller to [activity]. Automatically unbinds on `ON_DESTROY`. Application code
     * never calls this directly; it's invoked by the `PermissionManager(activity)` factory function.
     *
     * Registers directly against [ComponentActivity.getActivityResultRegistry] rather than through
     * the [ComponentActivity.registerForActivityResult] convenience wrapper, deliberately: that
     * wrapper refuses to register once the Activity is past `STARTED`, but Compose composition — and
     * therefore [io.github.ahmadhussain0335.kmppermission.compose.rememberPermissionState] et al — routinely runs well
     * after that point. The registry's own `register` has no such restriction.
     */
    fun attach(activity: ComponentActivity) {
        AndroidPermissionMapper.appTargetSdk = activity.applicationInfo.targetSdkVersion
        lateinit var boundHost: Host
        val launcher = activity.activityResultRegistry.register(
            "io.github.ahmadhussain0335.kmppermission.request_multiple_permissions_${launcherKeySequence.getAndIncrement()}",
            ActivityResultContracts.RequestMultiplePermissions(),
        ) {
            val continuation = boundHost.pendingContinuation
            boundHost.pendingContinuation = null
            continuation?.resume(Unit)
        }
        boundHost = Host(activity, launcher)
        host = boundHost

        activity.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                // The result of an in-flight prompt is delivered to the recreated Activity's
                // registry, never back here, so the waiting caller is cancelled instead of left
                // suspended forever (e.g. a ViewModel scope that survives a configuration change).
                boundHost.pendingContinuation?.cancel()
                boundHost.pendingContinuation = null
                boundHost.launcher.unregister()
                if (host === boundHost) host = null
            }
        })
    }

    private companion object {
        val launcherKeySequence = AtomicInteger(0)

        /**
         * Process-wide: Android shows one permission dialog at a time, and separate managers bound
         * to the same Activity would otherwise launch over each other.
         */
        val requestMutex = Mutex()
    }

    actual override fun status(permission: Permission): PermissionStatus {
        val activeHost = host ?: throw PermissionException.ManagerNotAttached()
        ManifestPermissionChecker.requireDeclared(activeHost.activity, permission)
        return activeHost.resolver.resolve(permission, activeHost.activity)
    }

    actual override suspend fun request(permissions: Set<Permission>): Map<Permission, PermissionStatus> =
        // ActivityResultLauncher.launch must be called on the main thread.
        withContext(Dispatchers.Main.immediate) { requestOnMain(permissions) }

    private suspend fun requestOnMain(permissions: Set<Permission>): Map<Permission, PermissionStatus> {
        val activeHost = host ?: throw PermissionException.ManagerNotAttached()
        val activity = activeHost.activity
        permissions.forEach { ManifestPermissionChecker.requireDeclared(activity, it) }

        // Permissions with nothing to prompt for on this SDK resolve straight from the current state.
        val (prompted, unprompted) = permissions.partition {
            !AndroidPermissionMapper.isAutoGranted(it) && AndroidPermissionMapper.manifestPermissions(it).isNotEmpty()
        }
        val (separate, together) = prompted.partition { AndroidPermissionMapper.requiresSeparateRequest(it) }

        val results = mutableMapOf<Permission, PermissionStatus>()
        unprompted.forEach { results[it] = activeHost.resolver.resolve(it, activity) }

        requestMutex.withLock {
            if (together.isNotEmpty()) {
                val instant = launchAndAwait(activeHost, requestArray(activity, together))
                together.forEach { results[it] = activeHost.resolver.resolveAfterRequest(it, activity, instant) }
            }
            separate.forEach { permission ->
                // Background location is only offered once foreground location is granted, so ask
                // for that first, as iOS does with "When In Use" before "Always".
                var foregroundInstant = false
                if (!hasForegroundLocation(activity)) {
                    foregroundInstant = launchAndAwait(activeHost, foregroundLocationRequest(activity, permission))
                }
                results[permission] = if (hasForegroundLocation(activity)) {
                    val instant = launchAndAwait(activeHost, requestArray(activity, listOf(permission)))
                    activeHost.resolver.resolveAfterRequest(permission, activity, instant)
                } else {
                    // Foreground location was refused, so background location cannot be granted;
                    // report how the foreground request ended.
                    val foreground = activeHost.resolver.resolveAfterRequest(foregroundLocationPermission(activity), activity, foregroundInstant)
                    if (foreground == PermissionStatus.PermanentlyDenied) PermissionStatus.PermanentlyDenied else PermissionStatus.Denied
                }
            }
        }

        return results
    }

    private fun requestArray(activity: ComponentActivity, permissions: List<Permission>): Array<String> =
        permissions.flatMap { permission ->
            AndroidPermissionMapper.manifestPermissions(permission) +
                AndroidPermissionMapper.optionalManifestPermissions(permission)
                    .filter { ManifestPermissionChecker.isDeclared(activity, it) }
        }.distinct().toTypedArray()

    private fun hasForegroundLocation(activity: ComponentActivity): Boolean =
        AndroidPermissionMapper.foregroundLocationPermissions.any {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }

    private fun foregroundLocationRequest(activity: ComponentActivity, permission: Permission): Array<String> {
        val declared = AndroidPermissionMapper.foregroundLocationPermissions.filter { ManifestPermissionChecker.isDeclared(activity, it) }
        if (declared.isEmpty()) {
            throw PermissionException.ManifestPermissionMissing(permission, AndroidPermissionMapper.foregroundLocationPermissions)
        }
        return declared.toTypedArray()
    }

    /** The foreground location [Permission] whose manifest entries [foregroundLocationRequest] asked for. */
    private fun foregroundLocationPermission(activity: ComponentActivity): Permission =
        if (ManifestPermissionChecker.isDeclared(activity, AndroidPermissionMapper.foregroundLocationPermissions.first())) {
            Permission.LocationFine
        } else {
            Permission.LocationCoarse
        }

    /** @return whether the result arrived too quickly for a dialog to have been answered. */
    private suspend fun launchAndAwait(host: Host, manifestPermissions: Array<String>): Boolean {
        // The Activity may have been destroyed between two launches of one request.
        if (this.host !== host) throw CancellationException("Host Activity was destroyed during the request.")
        val startedAt = SystemClock.elapsedRealtime()
        suspendCancellableCoroutine { continuation ->
            host.pendingContinuation = continuation
            continuation.invokeOnCancellation { host.pendingContinuation = null }
            host.launcher.launch(manifestPermissions)
        }
        return SystemClock.elapsedRealtime() - startedAt < AndroidStatusRules.AUTO_DENY_THRESHOLD_MS
    }

    actual override val foregroundEvents: Flow<Unit>
        get() = host?.activity?.let(::resumeEvents) ?: emptyFlow()

    private fun resumeEvents(activity: ComponentActivity): Flow<Unit> = callbackFlow {
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                trySend(Unit)
            }

            // Ends collection so a collector outliving the Activity (a ViewModel, say) doesn't keep it alive.
            override fun onDestroy(owner: LifecycleOwner) {
                this@callbackFlow.close()
            }
        }
        activity.lifecycle.addObserver(observer)
        awaitClose { activity.lifecycle.removeObserver(observer) }
    }.flowOn(Dispatchers.Main.immediate) // Lifecycle observers may only be added on the main thread.

    actual override fun openSettings() {
        val activeHost = host ?: throw PermissionException.ManagerNotAttached()
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activeHost.activity.packageName, null)
        }
        activeHost.activity.startActivity(intent)
    }
}
