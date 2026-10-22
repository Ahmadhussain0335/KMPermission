package com.kmp.permission.internal

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.kmp.permission.Permission
import com.kmp.permission.PermissionStatus

/**
 * Derives a [PermissionStatus] from the OS's current grant state plus [RequestedPermissionsStore]'s
 * history — the shared logic behind both [PermissionController.status] and its post-request result
 * mapping. See [AndroidStatusRules] for the decision itself.
 */
internal class PermissionStatusResolver(
    private val context: Context,
    private val requestedStore: RequestedPermissionsStore,
) {

    fun resolve(permission: Permission, activity: ComponentActivity?): PermissionStatus {
        if (AndroidPermissionMapper.isAutoGrantedBelowRuntime(permission)) return PermissionStatus.Granted

        val granted = isGranted(permission)
        val canShowRationale = canShowRationale(permission, activity)
        val history = AndroidStatusRules.observed(granted, canShowRationale, requestedStore.history(permission))
        requestedStore.save(permission, history)
        return AndroidStatusRules.status(granted, canShowRationale, history)
    }

    /** Records that a request for [permission] just completed, then resolves its status. */
    fun resolveAfterRequest(permission: Permission, activity: ComponentActivity): PermissionStatus {
        if (AndroidPermissionMapper.isAutoGrantedBelowRuntime(permission)) return PermissionStatus.Granted

        val granted = isGranted(permission)
        val canShowRationale = canShowRationale(permission, activity)
        val history = AndroidStatusRules.afterRequest(granted, canShowRationale, requestedStore.history(permission))
        requestedStore.save(permission, history)
        return AndroidStatusRules.status(granted, canShowRationale, history)
    }

    private fun isGranted(permission: Permission): Boolean =
        AndroidPermissionMapper.manifestPermissions(permission).all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    private fun canShowRationale(permission: Permission, activity: ComponentActivity?): Boolean =
        activity != null &&
            AndroidPermissionMapper.manifestPermissions(permission).any { activity.shouldShowRequestPermissionRationale(it) }
}
