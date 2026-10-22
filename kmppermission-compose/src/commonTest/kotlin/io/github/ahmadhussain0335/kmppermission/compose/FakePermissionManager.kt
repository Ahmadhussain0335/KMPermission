package io.github.ahmadhussain0335.kmppermission.compose

import io.github.ahmadhussain0335.kmppermission.Permission
import io.github.ahmadhussain0335.kmppermission.PermissionException
import io.github.ahmadhussain0335.kmppermission.PermissionManager
import io.github.ahmadhussain0335.kmppermission.PermissionResult
import io.github.ahmadhussain0335.kmppermission.PermissionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow

/**
 * In-memory [PermissionManager]. Each permission's status lives in a [MutableStateFlow] that
 * [observe] exposes, so tests can simulate both request results and changes made in Settings.
 */
internal class FakePermissionManager : PermissionManager {

    private val statuses = mutableMapOf<Permission, MutableStateFlow<PermissionStatus>>()

    /** What [request] resolves each permission to; unset permissions resolve to Denied. */
    val requestOutcomes = mutableMapOf<Permission, PermissionResult>()

    /** When set, every call on this permission throws it, like a missing manifest or Info.plist entry. */
    val failures = mutableMapOf<Permission, PermissionException>()

    val requested = mutableListOf<Set<Permission>>()

    private fun flowFor(permission: Permission) =
        statuses.getOrPut(permission) { MutableStateFlow(PermissionStatus.NotDetermined) }

    /** Simulates a change outside the app, such as the user toggling the permission in Settings. */
    fun setStatus(permission: Permission, status: PermissionStatus) {
        flowFor(permission).value = status
    }

    private fun failIfConfigured(permission: Permission) {
        failures[permission]?.let { throw it }
    }

    override fun check(permission: Permission): PermissionStatus {
        failIfConfigured(permission)
        return flowFor(permission).value
    }

    override suspend fun request(permission: Permission): PermissionResult = request(*arrayOf(permission)).getValue(permission)

    override suspend fun request(vararg permissions: Permission): Map<Permission, PermissionResult> {
        permissions.forEach(::failIfConfigured)
        requested += permissions.toSet()
        return permissions.associateWith { permission ->
            val outcome = requestOutcomes[permission] ?: PermissionStatus.Denied
            flowFor(permission).value = outcome
            outcome
        }
    }

    override fun request(permission: Permission, onResult: (PermissionResult) -> Unit) =
        error("Callback requests are not used by the Compose layer")

    override fun request(permissions: List<Permission>, onResult: (Map<Permission, PermissionResult>) -> Unit) =
        error("Callback requests are not used by the Compose layer")

    override fun areGranted(vararg permissions: Permission): Boolean = permissions.all { check(it) == PermissionStatus.Granted }

    override fun openSettings() = Unit

    // Throws on collection rather than on call, as the real manager's observe() does when the
    // platform check fails.
    override fun observe(permission: Permission): Flow<PermissionStatus> = flow {
        failIfConfigured(permission)
        flowFor(permission).collect { emit(it) }
    }

    override fun observe(vararg permissions: Permission): Flow<Map<Permission, PermissionStatus>> =
        combine(permissions.map { permission -> observe(permission) }) { values -> permissions.zip(values).toMap() }
}
