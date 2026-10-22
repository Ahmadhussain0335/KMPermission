package com.kmp.permission

/**
 * A single runtime permission the library knows how to check, request, and observe.
 *
 * Every case maps to one or more platform-specific identifiers — an Android manifest permission
 * string, an iOS `Info.plist` usage description key, or both — established by the platform engines.
 * Application code never needs to know those identifiers; declaring [Permission.Camera] is enough
 * regardless of platform, as long as the corresponding manifest/plist entry is also present.
 *
 * [Permission] is `sealed` rather than an `enum class` so the library can grow new cases across minor
 * releases without breaking binary compatibility of `when` expressions that already cover an `else`
 * branch.
 *
 * @property group the category this permission belongs to.
 */
sealed class Permission(val group: PermissionGroup) {

    /** The device camera(s). Android: `CAMERA`. iOS: `NSCameraUsageDescription`. */
    data object Camera : Permission(PermissionGroup.Camera)

    /** The microphone. Android: `RECORD_AUDIO`. iOS: `NSMicrophoneUsageDescription`. */
    data object Microphone : Permission(PermissionGroup.Microphone)

    /**
     * Legacy Bluetooth access. Android (API 30 and below): `BLUETOOTH`/`BLUETOOTH_ADMIN`. iOS:
     * `NSBluetoothAlwaysUsageDescription` (Core Bluetooth).
     */
    data object Bluetooth : Permission(PermissionGroup.Bluetooth)

    /** Scanning for nearby Bluetooth devices. Android 12+ (API 31+) only: `BLUETOOTH_SCAN`. */
    data object BluetoothScan : Permission(PermissionGroup.Bluetooth)

    /** Connecting to already-paired Bluetooth devices. Android 12+ (API 31+) only: `BLUETOOTH_CONNECT`. */
    data object BluetoothConnect : Permission(PermissionGroup.Bluetooth)

    /** Precise device location. Android: `ACCESS_FINE_LOCATION`. iOS: `NSLocationWhenInUseUsageDescription`. */
    data object LocationFine : Permission(PermissionGroup.Location)

    /** Approximate device location. Android: `ACCESS_COARSE_LOCATION`. iOS shares [LocationFine]'s authorization. */
    data object LocationCoarse : Permission(PermissionGroup.Location)

    /** Location access while backgrounded. Android: `ACCESS_BACKGROUND_LOCATION`. iOS: `NSLocationAlwaysAndWhenInUseUsageDescription`. */
    data object LocationBackground : Permission(PermissionGroup.Location)

    /** Posting local/push notifications. Android 13+ (API 33+): `POST_NOTIFICATIONS`. iOS: `UNUserNotificationCenter` authorization. */
    data object Notifications : Permission(PermissionGroup.Notifications)

    /** The user's contacts. Android: `READ_CONTACTS`. iOS: `NSContactsUsageDescription`. */
    data object Contacts : Permission(PermissionGroup.Contacts)

    /** The user's calendar. Android: `READ_CALENDAR`. iOS: `NSCalendarsUsageDescription`. */
    data object Calendar : Permission(PermissionGroup.Calendar)

    /** General file/storage access. Android: `READ_EXTERNAL_STORAGE` pre-API 33, scoped storage otherwise. Unsupported on iOS. */
    data object Storage : Permission(PermissionGroup.Storage)

    /** The photo/video library. Android 13+ (API 33+): `READ_MEDIA_IMAGES`/`READ_MEDIA_VIDEO`. iOS: `NSPhotoLibraryUsageDescription`. */
    data object Photos : Permission(PermissionGroup.Photos)

    /** Near-field communication. Android only: `NFC`. Unsupported on iOS. */
    data object Nfc : Permission(PermissionGroup.Nfc)

    /** Motion and fitness data. iOS only: `NSMotionUsageDescription` (Core Motion). Unsupported on Android. */
    data object Motion : Permission(PermissionGroup.Motion)

    /** Speech-to-text transcription authorization. iOS only: `NSSpeechRecognitionUsageDescription`. On Android this is covered by [Microphone]. */
    data object SpeechRecognition : Permission(PermissionGroup.SpeechRecognition)
}
