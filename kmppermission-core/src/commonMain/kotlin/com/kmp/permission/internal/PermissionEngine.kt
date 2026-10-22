package com.kmp.permission.internal

import com.kmp.permission.Permission
import com.kmp.permission.PermissionStatus

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
}
