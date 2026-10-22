package com.kmp.permission.internal

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.kmp.permission.Permission
import com.kmp.permission.PermissionException
import com.kmp.permission.PermissionStatus
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
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
     * therefore [com.kmp.permission.compose.rememberPermissionState] et al — routinely runs well
     * after that point. The registry's own `register` has no such restriction.
     */
    fun attach(activity: ComponentActivity) {
        lateinit var boundHost: Host
        val launcher = activity.activityResultRegistry.register(
            "com.kmp.permission.request_multiple_permissions_${launcherKeySequence.getAndIncrement()}",
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
        permissions.forEach { ManifestPermissionChecker.requireDeclared(activeHost.activity, it) }

        val (autoGranted, needsPrompt) = permissions.partition { AndroidPermissionMapper.isAutoGrantedBelowRuntime(it) }

        val results = mutableMapOf<Permission, PermissionStatus>()
        autoGranted.forEach { results[it] = PermissionStatus.Granted }

        if (needsPrompt.isNotEmpty()) {
            val manifestPermissions = needsPrompt
                .flatMap { AndroidPermissionMapper.manifestPermissions(it) }
                .distinct()
                .toTypedArray()

            requestMutex.withLock { launchAndAwait(activeHost, manifestPermissions) }

            needsPrompt.forEach { results[it] = activeHost.resolver.resolveAfterRequest(it, activeHost.activity) }
        }

        return results
    }

    private suspend fun launchAndAwait(host: Host, manifestPermissions: Array<String>) {
        suspendCancellableCoroutine { continuation ->
            host.pendingContinuation = continuation
            continuation.invokeOnCancellation { host.pendingContinuation = null }
            host.launcher.launch(manifestPermissions)
        }
    }

    actual override fun openSettings() {
        val activeHost = host ?: throw PermissionException.ManagerNotAttached()
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", activeHost.activity.packageName, null)
        }
        activeHost.activity.startActivity(intent)
    }
}
