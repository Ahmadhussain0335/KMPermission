package com.kmp.permission.internal

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.kmp.permission.Permission
import com.kmp.permission.PermissionStatus

/**
 * Derives a [PermissionStatus] from the OS's current grant state plus [RequestedPermissionsStore]'s
 * memory of whether the permission was ever asked for — the shared logic behind both
 * [PermissionController.status] and its post-request result mapping.
 */
internal class PermissionStatusResolver(
    private val context: Context,
    private val requestedStore: RequestedPermissionsStore,
) {

    fun resolve(permission: Permission, activity: ComponentActivity?): PermissionStatus {
        if (AndroidPermissionMapper.isAutoGrantedBelowRuntime(permission)) return PermissionStatus.Granted

        val manifestPermissions = AndroidPermissionMapper.manifestPermissions(permission)
        val allGranted = manifestPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (allGranted) return PermissionStatus.Granted

        if (!requestedStore.wasRequested(permission)) return PermissionStatus.NotDetermined

        val canShowRationale = activity != null &&
            manifestPermissions.any { activity.shouldShowRequestPermissionRationale(it) }
        return if (canShowRationale) PermissionStatus.Denied else PermissionStatus.PermanentlyDenied
    }
}
