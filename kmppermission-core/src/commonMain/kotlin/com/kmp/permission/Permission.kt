package com.kmp.permission

/**
 * A single runtime permission the library knows how to check, request, and observe.
 *
 * Every case maps to one or more platform-specific identifiers — an Android manifest permission
 * string, an iOS `Info.plist` usage description key, or both — established by the platform engines.
 * Application code never needs to know those identifiers; declaring [Permission.Camera] is enough
 * regardless of platform, as long as the corresponding manifest/plist entry is also present.
 *
 * [Permission] is `sealed` rather than an `enum class` so cases can carry their own documentation and
 * the type can grow. Adding a case is still source-breaking for exhaustive `when` expressions that
 * have no `else` branch.
 *
 * @property group the category this permission belongs to.
 */
public sealed class Permission(public val group: PermissionGroup) {

    /** The device camera(s). Android: `CAMERA`. iOS: `NSCameraUsageDescription`. */
    public data object Camera : Permission(PermissionGroup.Camera)

    /** The microphone. Android: `RECORD_AUDIO`. iOS: `NSMicrophoneUsageDescription`. */
    public data object Microphone : Permission(PermissionGroup.Microphone)

    /**
     * General Bluetooth access. Android API 31+: `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT`; API 30 and
     * below: the install-time `BLUETOOTH`/`BLUETOOTH_ADMIN` plus the location permission scanning
     * needs there (see [BluetoothScan]). iOS: `NSBluetoothAlwaysUsageDescription` (Core Bluetooth).
     */
    public data object Bluetooth : Permission(PermissionGroup.Bluetooth)

    /**
     * Scanning for nearby Bluetooth devices. Android API 31+: `BLUETOOTH_SCAN`. Below that, BLE scans
     * return no results without location permission, so this maps to `ACCESS_FINE_LOCATION` on API
     * 29–30 and `ACCESS_COARSE_LOCATION` on API 26–28.
     */
    public data object BluetoothScan : Permission(PermissionGroup.Bluetooth)

    /** Connecting to already-paired Bluetooth devices. Android API 31+: `BLUETOOTH_CONNECT`; always granted below that. */
    public data object BluetoothConnect : Permission(PermissionGroup.Bluetooth)

    /**
     * Making the device discoverable to other Bluetooth devices (BLE advertising). Android API 31+:
     * `BLUETOOTH_ADVERTISE`; below that the install-time `BLUETOOTH_ADMIN` covers it, so it is always
     * granted. iOS: `NSBluetoothAlwaysUsageDescription`, sharing [Bluetooth]'s authorization.
     */
    public data object BluetoothAdvertise : Permission(PermissionGroup.Bluetooth)

    /**
     * Discovering and connecting to nearby devices over Wi-Fi (Wi-Fi Direct, Wi-Fi Aware, scan
     * results, local-only hotspots). Android API 33+: `NEARBY_WIFI_DEVICES`; below that these APIs
     * need `ACCESS_FINE_LOCATION`, which this maps to. Unsupported on iOS.
     */
    public data object NearbyWifiDevices : Permission(PermissionGroup.Network)

    /**
     * Connecting to devices on the local network (TCP/UDP to local addresses, multicast, mDNS).
     * Android 17+ for apps targeting SDK 37+: `ACCESS_LOCAL_NETWORK`. Apps targeting a lower SDK get
     * an implicit grant, so this is [PermissionStatus.Granted] for them and on older Android; do not
     * declare `ACCESS_LOCAL_NETWORK` until the app targets SDK 37.
     *
     * Unsupported on iOS: there is no API to read local network authorization. iOS shows its prompt
     * (`NSLocalNetworkUsageDescription`) on the first local network connection; handle failures there.
     */
    public data object LocalNetwork : Permission(PermissionGroup.Network)

    /**
     * Precise device location. Android: `ACCESS_FINE_LOCATION`, plus `ACCESS_COARSE_LOCATION` on API
     * 31+, which the system requires in the same request. iOS: `NSLocationWhenInUseUsageDescription`.
     * Reported as [PermissionStatus.Limited] when the user allowed only approximate location.
     */
    public data object LocationFine : Permission(PermissionGroup.Location)

    /** Approximate device location. Android: `ACCESS_COARSE_LOCATION`. iOS shares [LocationFine]'s authorization. */
    public data object LocationCoarse : Permission(PermissionGroup.Location)

    /**
     * Location access while backgrounded. Android API 29+: `ACCESS_BACKGROUND_LOCATION`, requested
     * after foreground location (requesting it asks for foreground location first if needed; on API
     * 30+ the system shows a Settings page for it); below API 29 foreground location covers it. iOS:
     * both `NSLocationWhenInUseUsageDescription` and `NSLocationAlwaysAndWhenInUseUsageDescription`.
     */
    public data object LocationBackground : Permission(PermissionGroup.Location)

    /**
     * Posting local/push notifications. Android 13+ (API 33+): `POST_NOTIFICATIONS`; below that,
     * reflects whether notifications are enabled in Settings. iOS: `UNUserNotificationCenter`
     * authorization.
     */
    public data object Notifications : Permission(PermissionGroup.Notifications)

    /** The user's contacts. Android: `READ_CONTACTS`. iOS: `NSContactsUsageDescription`. */
    public data object Contacts : Permission(PermissionGroup.Contacts)

    /**
     * The user's calendar. Android: `READ_CALENDAR`. iOS 17+: `NSCalendarsFullAccessUsageDescription`;
     * earlier iOS: `NSCalendarsUsageDescription`.
     */
    public data object Calendar : Permission(PermissionGroup.Calendar)

    /**
     * Reading shared storage. Android below API 33: `READ_EXTERNAL_STORAGE`. API 33+ has no storage
     * permission (apps use scoped storage and the system pickers; use [Photos] for media), so this
     * is always [PermissionStatus.Granted] there. Unsupported on iOS.
     */
    public data object Storage : Permission(PermissionGroup.Storage)

    /**
     * The photo/video library. Android 13+ (API 33+): `READ_MEDIA_IMAGES`/`READ_MEDIA_VIDEO`; below
     * that `READ_EXTERNAL_STORAGE`. On API 34+, also declare `READ_MEDIA_VISUAL_USER_SELECTED` to
     * support selected-photos access, reported as [PermissionStatus.Limited]. iOS:
     * `NSPhotoLibraryUsageDescription`.
     */
    public data object Photos : Permission(PermissionGroup.Photos)

    /** Near-field communication. Android only: `NFC`. Unsupported on iOS. */
    public data object Nfc : Permission(PermissionGroup.Nfc)

    /** Motion and fitness data. iOS only: `NSMotionUsageDescription` (Core Motion). Unsupported on Android. */
    public data object Motion : Permission(PermissionGroup.Motion)

    /** Speech-to-text transcription authorization. iOS only: `NSSpeechRecognitionUsageDescription`. On Android this is covered by [Microphone]. */
    public data object SpeechRecognition : Permission(PermissionGroup.SpeechRecognition)
}
