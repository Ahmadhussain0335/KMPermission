package io.github.ahmadhussain0335.kmppermission.internal

import io.github.ahmadhussain0335.kmppermission.Permission
import io.github.ahmadhussain0335.kmppermission.PermissionStatus
import kotlinx.coroutines.flow.Flow

/**
 * Platform bridge consumed by [DefaultPermissionManager]. Android and iOS each supply the real
 * permission-checking/requesting logic here — Activity Result API on Android,
 * AVFoundation/CoreLocation/etc. on iOS.
 *
 * Not part of the public API: application code never touches this type directly.
 */
internal expect class PermissionController() : PermissionEngine {

    override fun status(permission: Permission): PermissionStatus

    override suspend fun request(permissions: Set<Permission>): Map<Permission, PermissionStatus>

    override fun openSettings()

    override val foregroundEvents: Flow<Unit>
}
