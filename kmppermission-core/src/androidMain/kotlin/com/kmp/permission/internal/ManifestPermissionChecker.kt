package com.kmp.permission.internal

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.kmp.permission.Permission
import com.kmp.permission.PermissionException

/**
 * Verifies the host app's `AndroidManifest.xml` actually declares what [permission] needs before the
 * library tries to check or request it — surfacing a missing `<uses-permission>` entry as a clear
 * developer error rather than a confusing platform-level failure.
 */
internal object ManifestPermissionChecker {

    fun requireDeclared(context: Context, permission: Permission) {
        val required = AndroidPermissionMapper.manifestPermissions(permission)
        if (required.isEmpty()) return

        val declared = declaredPermissions(context)
        val missing = required.filterNot { it in declared }
        if (missing.isNotEmpty()) {
            throw PermissionException.ManifestPermissionMissing(permission, missing)
        }
    }

    private fun declaredPermissions(context: Context): Set<String> {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        }
        return packageInfo.requestedPermissions?.toSet() ?: emptySet()
    }
}
