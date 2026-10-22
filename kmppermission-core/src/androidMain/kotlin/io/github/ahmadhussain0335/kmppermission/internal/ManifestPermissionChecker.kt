package io.github.ahmadhussain0335.kmppermission.internal

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import io.github.ahmadhussain0335.kmppermission.Permission
import io.github.ahmadhussain0335.kmppermission.PermissionException

/**
 * Verifies the host app's `AndroidManifest.xml` actually declares what [permission] needs before the
 * library tries to check or request it — surfacing a missing `<uses-permission>` entry as a clear
 * developer error rather than a confusing platform-level failure.
 */
internal object ManifestPermissionChecker {

    fun requireDeclared(context: Context, permission: Permission) {
        val required = AndroidPermissionMapper.manifestPermissions(permission)
        if (required.isEmpty()) return

        val missing = required.filterNot { isDeclared(context, it) }
        if (missing.isNotEmpty()) {
            throw PermissionException.ManifestPermissionMissing(permission, missing)
        }
    }

    fun isDeclared(context: Context, manifestPermission: String): Boolean =
        manifestPermission in declaredPermissions(context)

    // The declared set is fixed for the lifetime of the process, so the package manager IPC runs once.
    @Volatile
    private var cachedDeclared: Set<String>? = null

    private fun declaredPermissions(context: Context): Set<String> =
        cachedDeclared ?: readDeclaredPermissions(context).also { cachedDeclared = it }

    private fun readDeclaredPermissions(context: Context): Set<String> {
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
