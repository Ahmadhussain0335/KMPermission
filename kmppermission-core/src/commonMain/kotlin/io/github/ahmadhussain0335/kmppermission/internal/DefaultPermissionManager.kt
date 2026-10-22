package io.github.ahmadhussain0335.kmppermission.internal

import io.github.ahmadhussain0335.kmppermission.Permission
import io.github.ahmadhussain0335.kmppermission.PermissionManager
import io.github.ahmadhussain0335.kmppermission.PermissionResult
import io.github.ahmadhussain0335.kmppermission.PermissionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch

/**
 * The only implementation of [PermissionManager]. Fully platform-agnostic: all OS-specific work is
 * delegated to [engine], with this class contributing the orchestration — validation, exception
 * translation, and keeping [registry] (and therefore every active [observe] collector) in sync.
 *
 * @param scope used to bridge the callback-style [request] overloads onto the suspend engine calls.
 *   Callers are expected to supply a scope tied to their own lifecycle (an Activity's `lifecycleScope`
 *   on Android, for instance) so in-flight requests don't outlive their host screen.
 */
internal class DefaultPermissionManager(
    private val engine: PermissionEngine,
    private val registry: PermissionRegistry,
    private val scope: CoroutineScope,
) : PermissionManager {

    override suspend fun request(permission: Permission): PermissionResult {
        PermissionValidator.requireSupported(permission)
        val result = engine.request(setOf(permission)).getValue(permission)
        registry.update(permission, result)
        return result
    }

    override suspend fun request(vararg permissions: Permission): Map<Permission, PermissionResult> {
        val requested = permissions.toSet()
        PermissionValidator.requireNotEmpty(requested)
        requested.forEach(PermissionValidator::requireSupported)

        val results = engine.request(requested)
        registry.update(results)
        return results
    }

    override fun request(permission: Permission, onResult: (PermissionResult) -> Unit) {
        scope.launch { onResult(request(permission)) }
    }

    override fun request(permissions: List<Permission>, onResult: (Map<Permission, PermissionResult>) -> Unit) {
        scope.launch { onResult(request(*permissions.toTypedArray())) }
    }

    override fun check(permission: Permission): PermissionStatus {
        PermissionValidator.requireSupported(permission)
        val status = engine.status(permission)
        registry.update(permission, status)
        return status
    }

    override fun areGranted(vararg permissions: Permission): Boolean {
        PermissionValidator.requireNotEmpty(permissions.asList())
        return permissions.all { check(it) == PermissionStatus.Granted }
    }

    override fun openSettings() {
        engine.openSettings()
    }

    override fun observe(permission: Permission): Flow<PermissionStatus> {
        PermissionValidator.requireSupported(permission)
        val observed = setOf(permission)
        return merge(registry.statusFlow(permission), refreshOnForeground(observed))
            .onStart { refresh(observed) }
    }

    override fun observe(vararg permissions: Permission): Flow<Map<Permission, PermissionStatus>> {
        val requested = permissions.toSet()
        PermissionValidator.requireNotEmpty(requested)
        requested.forEach(PermissionValidator::requireSupported)
        return merge(registry.statusFlow(requested), refreshOnForeground(requested))
            .onStart { refresh(requested) }
    }

    /** Re-reads [permissions] whenever the app is foregrounded; emits nothing itself. */
    private fun refreshOnForeground(permissions: Set<Permission>): Flow<Nothing> =
        engine.foregroundEvents.transform { refresh(permissions) }

    private fun refresh(permissions: Set<Permission>) {
        permissions.forEach { registry.update(it, engine.status(it)) }
    }
}
