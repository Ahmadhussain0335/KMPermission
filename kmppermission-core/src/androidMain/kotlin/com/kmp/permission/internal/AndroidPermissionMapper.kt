package com.kmp.permission.internal

import android.os.Build
import com.kmp.permission.Permission

/**
 * Layers Android SDK-version gating on top of the platform-agnostic [PermissionMapper].
 *
 * A handful of permissions only became real runtime permissions on a specific API level — below
 * that level they're either not a concept yet (Bluetooth Scan/Connect, Notifications) or covered by
 * a different, older permission (Photos falling back to `READ_EXTERNAL_STORAGE`).
 */
internal object AndroidPermissionMapper {

    /** Manifest permission string(s) [permission] requires on [sdkInt]. Empty if none are needed there. */
    fun manifestPermissions(permission: Permission, sdkInt: Int = Build.VERSION.SDK_INT): List<String> =
        when (permission) {
            Permission.Photos ->
                if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
                    PermissionMapper.androidManifestPermissions(permission)
                } else {
                    listOf("android.permission.READ_EXTERNAL_STORAGE")
                }

            Permission.BluetoothScan, Permission.BluetoothConnect ->
                if (sdkInt >= Build.VERSION_CODES.S) PermissionMapper.androidManifestPermissions(permission) else emptyList()

            Permission.Notifications ->
                if (sdkInt >= Build.VERSION_CODES.TIRAMISU) PermissionMapper.androidManifestPermissions(permission) else emptyList()

            else -> PermissionMapper.androidManifestPermissions(permission)
        }

    /**
     * True if [permission] needs no system prompt on [sdkInt] at all — either it's a normal
     * (install-time) permission ([Permission.Bluetooth]), or [sdkInt] predates it being a runtime
     * permission at all (Bluetooth Scan/Connect pre-31, Notifications pre-33).
     */
    fun isAutoGrantedBelowRuntime(permission: Permission, sdkInt: Int = Build.VERSION.SDK_INT): Boolean =
        when (permission) {
            Permission.Bluetooth -> true
            Permission.BluetoothScan, Permission.BluetoothConnect -> sdkInt < Build.VERSION_CODES.S
            Permission.Notifications -> sdkInt < Build.VERSION_CODES.TIRAMISU
            else -> false
        }
}
