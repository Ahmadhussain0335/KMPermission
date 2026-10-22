package io.github.ahmadhussain0335.kmppermission.internal

import io.github.ahmadhussain0335.kmppermission.Permission
import io.github.ahmadhussain0335.kmppermission.PermissionException

/** Pre-flight checks shared by every [DefaultPermissionManager] entry point. */
internal object PermissionValidator {

    /** @throws IllegalArgumentException if [permissions] is empty. */
    fun requireNotEmpty(permissions: Collection<Permission>) {
        require(permissions.isNotEmpty()) { "At least one Permission must be provided." }
    }

    /** @throws PermissionException.UnsupportedPermission if [permission] does not exist on [currentPlatform]. */
    fun requireSupported(permission: Permission) {
        if (currentPlatform !in PermissionMapper.supportedPlatforms(permission)) {
            throw PermissionException.UnsupportedPermission(permission, currentPlatform.displayName)
        }
    }
}
