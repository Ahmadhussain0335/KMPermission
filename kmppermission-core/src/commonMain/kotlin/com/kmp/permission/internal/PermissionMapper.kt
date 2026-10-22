package com.kmp.permission.internal

import com.kmp.permission.Permission

/**
 * Static knowledge of which platforms each [Permission] exists on, and which platform-specific
 * identifier(s) — Android manifest permission strings, iOS `Info.plist` usage-description keys — it
 * requires there.
 *
 * Centralizing this here means the Android and iOS runtime engines (Milestones 4 and 5) consume one
 * shared source of truth instead of redeclaring these strings themselves.
 */
internal object PermissionMapper {

    /** Every [Permission] the library knows about. */
    val allPermissions: List<Permission> = listOf(
        Permission.Camera,
        Permission.Microphone,
        Permission.Bluetooth,
        Permission.BluetoothScan,
        Permission.BluetoothConnect,
        Permission.LocationFine,
        Permission.LocationCoarse,
        Permission.LocationBackground,
        Permission.Notifications,
        Permission.Contacts,
        Permission.Calendar,
        Permission.Storage,
        Permission.Photos,
        Permission.Nfc,
        Permission.Motion,
        Permission.SpeechRecognition,
    )

    /** The platforms [permission] exists on at all. */
    fun supportedPlatforms(permission: Permission): Set<Platform> = when (permission) {
        Permission.Nfc, Permission.Storage -> setOf(Platform.Android)
        Permission.Motion, Permission.SpeechRecognition -> setOf(Platform.Ios)
        else -> setOf(Platform.Android, Platform.Ios)
    }

    /** Android manifest permission string(s) [permission] requires. Empty if unsupported on Android. */
    fun androidManifestPermissions(permission: Permission): List<String> = when (permission) {
        Permission.Camera -> listOf("android.permission.CAMERA")
        Permission.Microphone -> listOf("android.permission.RECORD_AUDIO")
        Permission.Bluetooth -> listOf("android.permission.BLUETOOTH", "android.permission.BLUETOOTH_ADMIN")
        Permission.BluetoothScan -> listOf("android.permission.BLUETOOTH_SCAN")
        Permission.BluetoothConnect -> listOf("android.permission.BLUETOOTH_CONNECT")
        Permission.LocationFine -> listOf("android.permission.ACCESS_FINE_LOCATION")
        Permission.LocationCoarse -> listOf("android.permission.ACCESS_COARSE_LOCATION")
        Permission.LocationBackground -> listOf("android.permission.ACCESS_BACKGROUND_LOCATION")
        Permission.Notifications -> listOf("android.permission.POST_NOTIFICATIONS")
        Permission.Contacts -> listOf("android.permission.READ_CONTACTS")
        Permission.Calendar -> listOf("android.permission.READ_CALENDAR")
        Permission.Storage -> listOf("android.permission.READ_EXTERNAL_STORAGE")
        Permission.Photos -> listOf("android.permission.READ_MEDIA_IMAGES", "android.permission.READ_MEDIA_VIDEO")
        Permission.Nfc -> listOf("android.permission.NFC")
        Permission.Motion, Permission.SpeechRecognition -> emptyList()
    }

    /**
     * Every iOS `Info.plist` usage-description key [permission] requires on [iosMajorVersion]. Empty
     * either because [permission] is unsupported on iOS, or because it needs no usage description at
     * all (e.g. [Permission.Notifications], which is authorized via `UNUserNotificationCenter`).
     */
    fun iosInfoPlistKeys(permission: Permission, iosMajorVersion: Int): List<String> = when (permission) {
        Permission.Camera -> listOf("NSCameraUsageDescription")
        Permission.Microphone -> listOf("NSMicrophoneUsageDescription")
        Permission.Bluetooth, Permission.BluetoothScan, Permission.BluetoothConnect ->
            listOf("NSBluetoothAlwaysUsageDescription")
        Permission.LocationFine, Permission.LocationCoarse -> listOf("NSLocationWhenInUseUsageDescription")
        // "Always" is granted as an upgrade from "When In Use", so iOS requires both strings.
        Permission.LocationBackground ->
            listOf("NSLocationWhenInUseUsageDescription", "NSLocationAlwaysAndWhenInUseUsageDescription")
        Permission.Notifications -> emptyList()
        Permission.Contacts -> listOf("NSContactsUsageDescription")
        // iOS 17 replaced the single calendar key with separate full-access and write-only keys; the
        // library requests full access there.
        Permission.Calendar ->
            if (iosMajorVersion >= 17) listOf("NSCalendarsFullAccessUsageDescription") else listOf("NSCalendarsUsageDescription")
        Permission.Storage -> emptyList()
        Permission.Photos -> listOf("NSPhotoLibraryUsageDescription")
        Permission.Nfc -> emptyList()
        Permission.Motion -> listOf("NSMotionUsageDescription")
        Permission.SpeechRecognition -> listOf("NSSpeechRecognitionUsageDescription")
    }
}
