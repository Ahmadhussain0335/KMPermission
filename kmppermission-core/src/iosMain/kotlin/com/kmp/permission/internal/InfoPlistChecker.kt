package com.kmp.permission.internal

import com.kmp.permission.Permission
import com.kmp.permission.PermissionException
import platform.Foundation.NSBundle

/**
 * Verifies the host app's `Info.plist` actually declares every usage-description key [permission]
 * needs on the running iOS version before the library tries to check or request it. A missing key
 * makes iOS terminate the app when the prompt is shown, so this fails earlier with a clear message.
 */
internal object InfoPlistChecker {

    fun requireDeclared(permission: Permission) {
        val missing = PermissionMapper.iosInfoPlistKeys(permission, iosMajorVersion)
            .firstOrNull { NSBundle.mainBundle.objectForInfoDictionaryKey(it) == null }
            ?: return
        throw PermissionException.InfoPlistUsageDescriptionMissing(permission, missing)
    }
}
