# KMPermission

[![CI](https://github.com/Ahmadhussain0335/KMPermission/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/Ahmadhussain0335/KMPermission/actions/workflows/ci.yml)

A Kotlin Multiplatform library for checking, requesting and observing runtime permissions on Android and iOS through one API, with an optional Compose Multiplatform layer.

- 19 permissions, including the ones IoT and gateway apps need: Bluetooth scan/connect/advertise, nearby Wi-Fi and Android 17 local network access.
- Six statuses, including `Limited` for partial grants and a reliable `Denied` versus `PermanentlyDenied` split on Android.
- Suspend, callback and `Flow` APIs; `observe()` picks up changes made in system Settings.
- Handles platform differences for you: Android API 26 to 37 gating, the background-location sequence, and iOS 17/18 calendar and contacts changes.

## Modules

| Module | Contents |
|---|---|
| `kmppermission-core` | `PermissionManager`, `Permission`, `PermissionStatus`, and the Android and iOS engines. iOS framework: `KMPPermissionCore`. |
| `kmppermission-compose` | `rememberPermissionState`, `rememberMultiplePermissions`, `rememberPermissionLauncher`, `PermissionEffect`, `PermissionButton`. Depends on core via `api`. |
| `sample-android` | Compose sample app. |
| `sample-ios` | SwiftUI sample app using `KMPPermissionCore` (open `sample-ios/iosApp.xcodeproj`). |

The library supports Android 8.0 (API 26) and later and iOS 15 and later. It is not published to a repository yet; include the modules from source.

## Statuses

| Status | Meaning |
|---|---|
| `Granted` | The feature can be used. |
| `Limited` | Partly granted, so the feature works with reduced scope. Applies to: selected photos (iOS, Android 14+), limited contacts (iOS 18+), write-only calendar (iOS 17+), and approximate location for `LocationFine`. |
| `Denied` | Denied, but the system will prompt again. Android only. |
| `PermanentlyDenied` | The system will not prompt again; call `openSettings()`. On iOS every denial is permanent. |
| `Restricted` | Blocked by policy (MDM, parental controls). Neither requesting nor Settings will help. |
| `NotDetermined` | Never requested. |

`observe()` emits the current status, then again on every change. That includes changes made in system Settings: the status is re-checked each time the app returns to the foreground.

Configuration errors throw a `PermissionException` and are never reported as a status:
- `ManifestPermissionMissing`
- `InfoPlistUsageDescriptionMissing`
- `UnsupportedPermission`
- `ManagerNotAttached`

## Required declarations

Declare what you use; the library checks and fails with a message naming the missing entry.

| Permission | Android manifest | iOS Info.plist |
|---|---|---|
| `Camera` | `CAMERA` | `NSCameraUsageDescription` |
| `Microphone` | `RECORD_AUDIO` | `NSMicrophoneUsageDescription` |
| `Bluetooth` | API 31+: `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`. API 30 and below: `BLUETOOTH`, `BLUETOOTH_ADMIN` (declare with `maxSdkVersion="30"`) plus the scan location permission below | `NSBluetoothAlwaysUsageDescription` |
| `BluetoothScan` | API 31+: `BLUETOOTH_SCAN`. API 29-30: `ACCESS_FINE_LOCATION`. API 26-28: `ACCESS_COARSE_LOCATION` | `NSBluetoothAlwaysUsageDescription` |
| `BluetoothConnect` | `BLUETOOTH_CONNECT` (API 31+) | `NSBluetoothAlwaysUsageDescription` |
| `BluetoothAdvertise` | `BLUETOOTH_ADVERTISE` (API 31+) | `NSBluetoothAlwaysUsageDescription` |
| `NearbyWifiDevices` | API 33+: `NEARBY_WIFI_DEVICES` (add `usesPermissionFlags="neverForLocation"` if you never derive location). Below API 33: `ACCESS_FINE_LOCATION` | not supported |
| `LocalNetwork` | Android 17+ when the app targets SDK 37+: `ACCESS_LOCAL_NETWORK`. Do not declare it before targeting 37; until then it reports `Granted` | not supported (see iOS notes) |
| `LocationFine` | `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` | `NSLocationWhenInUseUsageDescription` |
| `LocationCoarse` | `ACCESS_COARSE_LOCATION` | `NSLocationWhenInUseUsageDescription` |
| `LocationBackground` | `ACCESS_BACKGROUND_LOCATION` and a foreground location permission | `NSLocationWhenInUseUsageDescription` and `NSLocationAlwaysAndWhenInUseUsageDescription` |
| `Notifications` | `POST_NOTIFICATIONS` (API 33+) | none |
| `Contacts` | `READ_CONTACTS` | `NSContactsUsageDescription` |
| `Calendar` | `READ_CALENDAR` | iOS 17+: `NSCalendarsFullAccessUsageDescription`; earlier: `NSCalendarsUsageDescription` |
| `Storage` | `READ_EXTERNAL_STORAGE` with `maxSdkVersion="32"` | not supported |
| `Photos` | `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`; optionally `READ_MEDIA_VISUAL_USER_SELECTED` (API 34+); below API 33 `READ_EXTERNAL_STORAGE` | `NSPhotoLibraryUsageDescription` |
| `Nfc` | `NFC` | not supported |
| `Motion` | not supported | `NSMotionUsageDescription` |
| `SpeechRecognition` | not supported (use `Microphone`) | `NSSpeechRecognitionUsageDescription` |

Usage strings must describe the real feature. App Review rejects generic text.

Below API 31, BLE scans return no results without location permission, so `BluetoothScan` and `Bluetooth` request it there. Declare `BLUETOOTH_SCAN` with `android:usesPermissionFlags="neverForLocation"` only if you never derive location from scan results.

## Usage

### Android

```kotlin
class MainActivity : ComponentActivity() {
    private val permissions by lazy { PermissionManager(this) }

    fun onScanClicked() = lifecycleScope.launch {
        val results = permissions.request(Permission.BluetoothScan, Permission.BluetoothConnect)
        if (results.values.all { it == PermissionStatus.Granted }) startScan()
    }
}
```

Create the manager per Activity instance. A request still waiting when its Activity is destroyed (for example on rotation) is cancelled.

### iOS (Swift)

```swift
let manager = PermissionManager_iosKt.PermissionManager()

manager.request(permissions: [Permission.Camera.shared, Permission.Microphone.shared]) { results in
    // results: [Permission: PermissionStatus]; delivered on the main thread
}
```

Requests run on the main thread one at a time, so issuing several in a row is safe.

### Compose

```kotlin
@Composable
fun CameraScreen() {
    val camera = rememberPermissionState(Permission.Camera)
    when (camera.status) {
        PermissionStatus.Granted -> CameraPreview()
        PermissionStatus.PermanentlyDenied -> OpenSettingsPrompt()
        else -> Button(onClick = camera::launchPermissionRequest) { Text("Allow camera") }
    }
}
```

## Platform notes

### Android

- `LocationBackground` is requested on its own after foreground location, and foreground location is asked for first if it isn't granted. On API 30+ the system shows a Settings page for it. Below API 29 foreground location covers it.
- `LocalNetwork` is only enforced on Android 17+ for apps targeting SDK 37+. When it is, a grant of another Nearby devices permission (Bluetooth Scan/Connect/Advertise, Nearby Wi-Fi) may cover it without a new prompt. On Android 16 the restriction is an opt-in developer compat flag (`RESTRICT_LOCAL_NETWORK`) and is not reflected by the library.
- `Storage` reports `Granted` on API 33+, where no storage permission exists. Use `Photos` or the system pickers for media.
- `Notifications` below API 33 reflects whether notifications are enabled in Settings.
- `Denied` versus `PermanentlyDenied` relies on a small history the library keeps in SharedPreferences (`com.kmp.permission.requested_permissions`). Exclude it from backup; see `sample-android/src/main/res/xml/`. Otherwise a restored install reports never-requested permissions as permanently denied.
- A system auto-deny is recognised because the result arrives within 500 ms, faster than a person can answer a dialog. A dismissed dialog stays `Denied`.

### iOS

- Requires iOS 15 or later (the Kotlin/Native framework's minimum).
- `LocationFine` reports `Limited` when Precise Location is off. Requesting again does not change that; the user has to turn Precise Location on in Settings. The library does not request temporary full accuracy.
- `Bluetooth`, `BluetoothScan`, `BluetoothConnect` and `BluetoothAdvertise` share one Core Bluetooth authorization.
- `LocalNetwork` is not supported. iOS has no API to read local network authorization; it shows its prompt on the app's first local network connection. Add `NSLocalNetworkUsageDescription` (and `NSBonjourServices` for any Bonjour types you browse or publish) and handle connection failures there.

## Building and testing

```sh
./gradlew :kmppermission-core:testAndroidHostTest                 # unit tests (common + Android)
./gradlew :sample-android:assembleDebug
./gradlew :kmppermission-core:compileKotlinIosSimulatorArm64      # iOS compile check
```

Running the shared tests on the iOS simulator (`:kmppermission-core:iosSimulatorArm64Test`) locally needs an Apple-silicon Mac; CI runs them on every push. To build the iOS sample, open `sample-ios/iosApp.xcodeproj` and set your team in `sample-ios/Configuration/Config.xcconfig`, or pass `TEAM_ID=<team>` to `xcodebuild`.

CI (`.github/workflows/ci.yml`) runs the host tests, lint, the shared tests on the iOS simulator, and builds both samples and the iOS UI tests on every push to `main` and every pull request.

### Device tests

`sample-ios/iosAppUITests` drives the iOS sample through real system permission alerts: denial, approximate location, declining and accepting the Always upgrade, Bluetooth, Contacts (including the iOS 18+ sharing sheet), Calendar, Photos, Notifications, Motion, Speech, and a change made in Settings. It resets permissions itself where XCUITest allows it. Run it on an unlocked device:

```sh
xcodebuild test -project sample-ios/iosApp.xcodeproj -scheme iosAppUITests \
  -destination 'platform=iOS,id=<device UDID>' TEAM_ID=<team> -allowProvisioningUpdates
```

With a free Personal Team, trust the developer certificate on the device first (Settings → General → VPN & Device Management).

## Project status

Version 0.1.0. Not yet published; the API may still change before 1.0.

Tested on real devices:
- Samsung Galaxy A53 (Android 16): denial and permanent denial, dismissed dialogs, approximate then precise location, the background-location sequence, selected photos, the Bluetooth and Nearby Wi-Fi group, notifications, contacts, calendar, changes made in Settings, and rotation during a dialog.
- iPhone 13 (iOS 26.6): the full `iosAppUITests` suite.

Not yet covered on a device: Android 12 and older, Android 17 with `LocalNetwork`, and on iOS "Allow Once", Location Services turned off, and the limited Photos and Contacts pickers.

Known limitations:
- Android: a system auto-deny is detected by timing (under 500 ms). If a user resets a once-denied permission to "Ask every time" in Settings, it reads as `PermanentlyDenied` until the next request.
- iOS: prompt detection relies on the app's active/inactive notifications with short timeouts. The synchronous `check()` for notifications can block for up to 500 ms; `request()` and `observe()` do not block.
- A request still waiting when its Android Activity is destroyed is cancelled (`CancellationException`), not resumed.

Planned:
- Publishing (Maven Central or an internal repository, with an XCFramework or Swift package for iOS), `explicitApi()` and binary-compatibility checks, and moving off the pre-release material3 and lifecycle dependencies.
- An instrumented Android UI test suite matching the iOS one, plus Compose and Robolectric tests.
- A Compose sample for iOS.
