package com.kmp.permission.internal

import com.kmp.permission.Permission
import com.kmp.permission.PermissionStatus

/**
 * Platform bridge consumed by the common [com.kmp.permission.PermissionManager] implementation built
 * in Milestone 3. Android and iOS each supply the real permission-checking/requesting logic here —
 * Activity Result API on Android, AVFoundation/CoreLocation/etc. on iOS — in Milestones 4 and 5.
 *
 * Not part of the public API: application code never touches this type directly.
 */
internal expect class PermissionController() : PermissionEngine {

    override fun status(permission: Permission): PermissionStatus

    override suspend fun request(permissions: Set<Permission>): Map<Permission, PermissionStatus>

    override fun openSettings()
}
