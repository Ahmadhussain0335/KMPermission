package com.kmp.permission.internal

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kmp.permission.Permission
import com.kmp.permission.PermissionStatus

/**
 * Derives a [PermissionStatus] from the OS's current grant state plus [RequestedPermissionsStore]'s
 * history — the shared logic behind both [PermissionController.status] and its post-request result
 * mapping. See [AndroidStatusRules] for the denial logic.
 */
internal class PermissionStatusResolver(
    private val context: Context,
    private val requestedStore: RequestedPermissionsStore,
) {

    fun resolve(permission: Permission, activity: ComponentActivity?): PermissionStatus =
        resolve(permission, activity, AndroidStatusRules::observed)

    /**
     * Records that a request for [permission] just completed, then resolves its status.
     *
     * @param answeredInstantly whether the result came back faster than
     *   [AndroidStatusRules.AUTO_DENY_THRESHOLD_MS], i.e. without a dialog being answered.
     */
    fun resolveAfterRequest(permission: Permission, activity: ComponentActivity, answeredInstantly: Boolean): PermissionStatus =
        resolve(permission, activity) { granted, canShowRationale, history ->
            AndroidStatusRules.afterRequest(granted, canShowRationale, history, answeredInstantly)
        }

    private fun resolve(
        permission: Permission,
        activity: ComponentActivity?,
        updateHistory: (granted: Boolean, canShowRationale: Boolean, history: RequestHistory) -> RequestHistory,
    ): PermissionStatus {
        if (AndroidPermissionMapper.isAutoGranted(permission)) return PermissionStatus.Granted
        if (permission == Permission.Notifications && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // No runtime permission exists; the user can only turn notifications off in Settings.
            return if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                PermissionStatus.Granted
            } else {
                PermissionStatus.PermanentlyDenied
            }
        }

        val fullyGranted = AndroidPermissionMapper.manifestPermissions(permission).all(::isGranted)
        val partiallyGranted = !fullyGranted && AndroidPermissionMapper.partialGrantPermissions(permission).any(::isGranted)
        val canShowRationale = canShowRationale(permission, activity)

        // A partial grant behaves like a grant for history: the system will prompt again to widen it.
        val history = requestedStore.update(permission) { updateHistory(fullyGranted || partiallyGranted, canShowRationale, it) }

        return if (partiallyGranted) {
            PermissionStatus.Limited
        } else {
            AndroidStatusRules.status(fullyGranted, canShowRationale, history)
        }
    }

    private fun isGranted(manifestPermission: String): Boolean =
        ContextCompat.checkSelfPermission(context, manifestPermission) == PackageManager.PERMISSION_GRANTED

    private fun canShowRationale(permission: Permission, activity: ComponentActivity?): Boolean =
        activity != null &&
            AndroidPermissionMapper.manifestPermissions(permission).any { activity.shouldShowRequestPermissionRationale(it) }
}
