package com.kmp.permission.internal

import android.os.Build
import com.kmp.permission.Permission

private const val ACCESS_FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION"
private const val ACCESS_COARSE_LOCATION = "android.permission.ACCESS_COARSE_LOCATION"
private const val READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE"
private const val READ_MEDIA_VISUAL_USER_SELECTED = "android.permission.READ_MEDIA_VISUAL_USER_SELECTED"

/**
 * Layers Android SDK-version gating on top of the platform-agnostic [PermissionMapper].
 *
 * Several permissions changed shape across API levels: some only became runtime permissions at a
 * specific level (Bluetooth Scan/Connect, Notifications), some are covered by a different permission
 * below or above a level (Photos, Storage, background location), and some can be granted partially
 * (approximate location, selected photos).
 */
internal object AndroidPermissionMapper {

    /**
     * Manifest permission string(s) that must all be declared and granted for [permission] to be
     * [com.kmp.permission.PermissionStatus.Granted] on [sdkInt]. Empty if none apply there.
     */
    fun manifestPermissions(permission: Permission, sdkInt: Int = Build.VERSION.SDK_INT): List<String> =
        when (permission) {
            Permission.Photos ->
                if (sdkInt >= Build.VERSION_CODES.TIRAMISU) {
                    PermissionMapper.androidManifestPermissions(permission)
                } else {
                    listOf(READ_EXTERNAL_STORAGE)
                }

            // From API 31 the legacy BLUETOOTH/BLUETOOTH_ADMIN permissions are ignored and usually
            // declared with maxSdkVersion="30"; general Bluetooth use needs Scan and Connect instead.
            Permission.Bluetooth ->
                if (sdkInt >= Build.VERSION_CODES.S) {
                    PermissionMapper.androidManifestPermissions(Permission.BluetoothScan) +
                        PermissionMapper.androidManifestPermissions(Permission.BluetoothConnect)
                } else {
                    PermissionMapper.androidManifestPermissions(permission)
                }

            Permission.BluetoothScan, Permission.BluetoothConnect ->
                if (sdkInt >= Build.VERSION_CODES.S) PermissionMapper.androidManifestPermissions(permission) else emptyList()

            // Below API 33 notifications have no runtime permission; see PermissionStatusResolver.
            Permission.Notifications ->
                if (sdkInt >= Build.VERSION_CODES.TIRAMISU) PermissionMapper.androidManifestPermissions(permission) else emptyList()

            // From API 31 the system ignores a request for fine location that doesn't also ask for
            // coarse, and the user may grant only the coarse one.
            Permission.LocationFine ->
                if (sdkInt >= Build.VERSION_CODES.S) listOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION) else listOf(ACCESS_FINE_LOCATION)

            // Below API 29 any foreground location grant also covers background access.
            Permission.LocationBackground ->
                if (sdkInt >= Build.VERSION_CODES.Q) PermissionMapper.androidManifestPermissions(permission) else listOf(ACCESS_FINE_LOCATION)

            // READ_EXTERNAL_STORAGE has no effect from API 33; apps use scoped storage, the
            // system pickers, or Photos for media.
            Permission.Storage ->
                if (sdkInt >= Build.VERSION_CODES.TIRAMISU) emptyList() else PermissionMapper.androidManifestPermissions(permission)

            else -> PermissionMapper.androidManifestPermissions(permission)
        }

    /**
     * Manifest permission(s) whose grant, without the full [manifestPermissions] set, means
     * [permission] is usable with reduced scope ([com.kmp.permission.PermissionStatus.Limited]).
     */
    fun partialGrantPermissions(permission: Permission, sdkInt: Int = Build.VERSION.SDK_INT): List<String> =
        when (permission) {
            Permission.LocationFine -> if (sdkInt >= Build.VERSION_CODES.S) listOf(ACCESS_COARSE_LOCATION) else emptyList()
            Permission.Photos -> optionalManifestPermissions(permission, sdkInt)
            else -> emptyList()
        }

    /**
     * Manifest permission(s) added to a request for [permission] only when the host app declares
     * them. Declaring `READ_MEDIA_VISUAL_USER_SELECTED` on API 34+ lets the user pick specific photos
     * and keeps that choice instead of granting temporary access.
     */
    fun optionalManifestPermissions(permission: Permission, sdkInt: Int = Build.VERSION.SDK_INT): List<String> =
        when (permission) {
            Permission.Photos ->
                if (sdkInt >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) listOf(READ_MEDIA_VISUAL_USER_SELECTED) else emptyList()
            else -> emptyList()
        }

    /**
     * True if [permission] needs no system prompt on [sdkInt] and is always available: either it's
     * a normal (install-time) permission there, or there is nothing left to grant at that level.
     */
    fun isAutoGranted(permission: Permission, sdkInt: Int = Build.VERSION.SDK_INT): Boolean =
        when (permission) {
            Permission.Bluetooth, Permission.BluetoothScan, Permission.BluetoothConnect -> sdkInt < Build.VERSION_CODES.S
            Permission.Storage -> sdkInt >= Build.VERSION_CODES.TIRAMISU
            else -> false
        }

    /**
     * True if [permission] has to be requested on its own, after foreground location is granted.
     * From API 29 background location is granted separately; from API 30 the system ignores a
     * request that mixes it with foreground location.
     */
    fun requiresSeparateRequest(permission: Permission, sdkInt: Int = Build.VERSION.SDK_INT): Boolean =
        permission == Permission.LocationBackground && sdkInt >= Build.VERSION_CODES.Q

    /** Foreground location permissions, one of which must be granted before background location. */
    val foregroundLocationPermissions: List<String> = listOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)
}
