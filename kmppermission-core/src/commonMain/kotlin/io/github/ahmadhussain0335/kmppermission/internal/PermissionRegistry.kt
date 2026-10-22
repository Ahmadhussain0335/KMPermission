package io.github.ahmadhussain0335.kmppermission.internal

import io.github.ahmadhussain0335.kmppermission.Permission
import io.github.ahmadhussain0335.kmppermission.PermissionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory, reactive cache of the last known [PermissionStatus] for each [Permission].
 *
 * This is the single place [DefaultPermissionManager] writes status updates to and the single place
 * [io.github.ahmadhussain0335.kmppermission.PermissionObserver.observe] reads them back from as a [Flow] — request/check
 * results and Settings-triggered changes all flow through here, so observers never miss an update.
 */
internal class PermissionRegistry {

    private val statuses = MutableStateFlow<Map<Permission, PermissionStatus>>(emptyMap())

    fun update(permission: Permission, status: PermissionStatus) {
        statuses.update { it + (permission to status) }
    }

    fun update(statuses: Map<Permission, PermissionStatus>) {
        this.statuses.update { it + statuses }
    }

    fun statusFlow(permission: Permission): Flow<PermissionStatus> =
        statuses.map { it[permission] ?: PermissionStatus.NotDetermined }.distinctUntilChanged()

    fun statusFlow(permissions: Set<Permission>): Flow<Map<Permission, PermissionStatus>> =
        statuses.map { current -> permissions.associateWith { current[it] ?: PermissionStatus.NotDetermined } }
            .distinctUntilChanged()
}
