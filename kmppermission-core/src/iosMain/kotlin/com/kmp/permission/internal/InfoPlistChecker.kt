package com.kmp.permission.internal

import com.kmp.permission.Permission
import com.kmp.permission.PermissionException
import platform.Foundation.NSBundle

/**
 * Verifies the host app's `Info.plist` actually declares the usage-description key [permission]
 * needs before the library tries to check or request it.
 */
internal object InfoPlistChecker {

    fun requireDeclared(permission: Permission) {
        val key = PermissionMapper.iosInfoPlistKeys(permission).firstOrNull() ?: return
        if (NSBundle.mainBundle.objectForInfoDictionaryKey(key) == null) {
            throw PermissionException.InfoPlistUsageDescriptionMissing(permission, key)
        }
    }
}
