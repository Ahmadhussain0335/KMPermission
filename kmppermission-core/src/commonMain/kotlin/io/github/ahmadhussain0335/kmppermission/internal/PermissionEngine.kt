package io.github.ahmadhussain0335.kmppermission.internal

import io.github.ahmadhussain0335.kmppermission.Permission
import io.github.ahmadhussain0335.kmppermission.PermissionStatus
import kotlinx.coroutines.flow.Flow

/**
 * Abstraction [DefaultPermissionManager] delegates the real permission work to.
 *
 * [PermissionController] is the production implementation of this interface (one `actual` per
 * platform). Kept as a separate interface — rather than depending on [PermissionController]
 * directly — so [DefaultPermissionManager] can be unit tested against a fake in `commonTest`
 * without an expect/actual class in the way.
 */
internal interface PermissionEngine {

    fun status(permission: Permission): PermissionStatus

    suspend fun request(permissions: Set<Permission>): Map<Permission, PermissionStatus>

    fun openSettings()

    /**
     * Emits each time the app returns to the foreground, which is when a change made in system
     * Settings becomes visible. Neither platform notifies apps of permission changes directly.
     */
    val foregroundEvents: Flow<Unit>
}
