# KMPermission: Project Status

Last updated: 2026-10-22. All work is on `main` at https://github.com/Ahmadhussain0335/KMPermission. CI runs on every push to `main` and every pull request.

## 1. Where things stand

| Area | State |
|---|---|
| Common API | 19 permissions (BluetoothAdvertise, NearbyWifiDevices and LocalNetwork added); 6 statuses, including the new `Limited`; check / request / observe / openSettings. |
| Android engine | Done. SDK-gated for API 26–36. |
| iOS engine | Done. Minimum iOS 15. |
| Compose layer | Done. Android verified on a device through the sample; iOS compiles but has never been run. |
| Samples | Android (Compose) and iOS (SwiftUI); both build and were exercised on real devices. |
| Tests | 64 host tests (also run on the iOS simulator in CI): 13 manager, 7 mapper, 27 Android mapper, 11 Android status rules, 3 registry, 3 validator. 10 iOS device UI tests in `sample-ios/iosAppUITests`. All passing. |
| Docs | README rewritten: setup table, usage, platform notes. |
| CI | GitHub Actions: Android tests, lint and sample; common tests on the iOS simulator; iOS framework, sample and UI-test build. |
| Publishing | Not started (deferred by decision). |

**Verified on 2026-10-22:**
- Gradle and Xcode builds as listed above, locally and in CI (both jobs green).
- **Samsung Galaxy A53 (SM-A536E), Android 16**, driven through the sample with adb:

| Scenario | Result |
|---|---|
| Deny, deny again, request a third time | Denied, then Blocked; the third request auto-denied without a dialog |
| Dismiss the dialog with Back, twice | Stays Denied |
| Approximate location, then upgrade to precise | Limited, then Granted |
| Background location from scratch | Foreground prompt, Settings page, Granted |
| Selected photos | Limited |
| Bluetooth, Advertise, Nearby Wi-Fi, Notifications, Contacts, Calendar | Granted (Advertise and Wi-Fi without a prompt: Android grants the Nearby devices group together) |
| Storage, NFC | Granted, no prompt |
| Allow, then revoke, in Settings | Granted on return; after revoke Denied, and the dialog shows again |
| Rotate while a dialog is open | No crash; Granted |

- **iPhone 13, iOS 26.6**, via `iosAppUITests` (10/10 passing in one run): camera denial, microphone, approximate location (Limited), declining the Always upgrade without hanging, When In Use followed by the Always upgrade, Bluetooth and Advertise, Contacts with the iOS 18+ sharing sheet, Calendar, Photos, Notifications/Motion/Speech, and a camera grant in Settings picked up on return.

**Bugs found on the devices, fixed and re-verified:**
- `c7c57fe` Android: a fresh install showed Background Location as Denied, because Android reports a rationale for it before it is ever requested.
- `8fa8e91` iOS: status was read from a second `CLLocationManager` that lags behind the delegate manager, so requesting background location right after When In Use skipped the Always prompt.
- `abfb51f` iOS: right after "Allow Full Access", EventKit still reported NotDetermined, so `request(Calendar)` returned NotDetermined. Handlers now fall back to the answer the prompt's completion reported.

**Not verified:**
- Android below API 33 (Bluetooth location mapping, Nearby Wi-Fi fallback, notifications setting) and Android 17 (`LocalNetwork`): no such devices were connected.
- iOS: Location Services turned off system-wide, "Allow Once", and the limited Photos/Contacts pickers (the tests choose full access).

## 2. Done in this pass

| Commit | Change |
|---|---|
| `7e1ca8a` | Batch `observe()` validated nothing, so Motion/Speech on Android came back Granted; it now validates platform support. |
| `16f95f5` | iOS requests run on main under one process-wide lock. Concurrent Bluetooth/Location requests no longer overwrite each other's continuation. Background location no longer hangs when no prompt can appear. |
| `063a8ea` | iOS checks every required Info.plist key, including `NSCalendarsFullAccessUsageDescription` on iOS 17+ and both keys for background location. |
| `68f19d0` | iOS sample: added the missing usage strings and Motion/Speech rows; each row is requested as one batch. |
| `c9d8a5a` | iOS notifications: removed `runBlocking` and `!!`. The synchronous check is bounded to 500 ms. |
| `40d4006` | Android: the dialog launches on main; one process-wide request lock. A pending request is cancelled, and its launcher unregistered, on destroy. |
| `59882e7` | Android: request history is recorded after the result instead of before the prompt. Preference keys use stable IDs, so they survive R8. |
| `ec3fcb5` | New `PermissionStatus.Limited`, set on iOS for limited Photos/Contacts, write-only Calendar and approximate location. **Breaking** for exhaustive `when`. |
| `2bb8ba0` | Android SDK gating: see the list below. |
| `7e285db` | `observe()` re-checks when the app returns to the foreground, so changes made in Settings now reach observers. |
| `80cb8be` | Compose: per-permission states in `rememberMultiplePermissions` are keyed. |
| `e751dc9` | Android sample manifest: see the list below. |
| `cfe64d1`, `c24af5e` | README and KDoc rewritten; stale milestone comments removed. |
| `c5cbaa6` | Review fix, iOS: location prompts wait for the app to be active and only follow a prompt actually seen to appear; When In Use is bounded too; Bluetooth gets a re-check when the app reactivates. |
| `199e725` | Review fix, Android: see the list below. |

`2bb8ba0`, Android SDK gating:
- Bluetooth on API 31+ maps to Scan + Connect.
- Storage reports Granted on API 33+, where no storage permission exists.
- Fine location is requested together with Coarse on API 31+; an approximate-only grant reports Limited.
- Background location is requested separately, foreground first.
- Selected photos on API 34+ reports Limited.
- Notifications below API 33 reflect the system notification setting.
- The declared-permissions list is cached instead of queried on every check.

`e751dc9`, Android sample manifest:
- Added `maxSdkVersion` to the legacy entries.
- Marked BLUETOOTH_SCAN `neverForLocation`.
- Declared selected-photos access and NFC.
- Hardware features are declared optional, which fixes the lint error.
- The library's preferences file is excluded from backup.

`199e725`, Android review fixes:
- A system auto-deny is now recognised by timing, so repeated dismissals no longer read as PermanentlyDenied.
- Background location reports the foreground outcome when foreground location was refused.
- The foreground-events flow closes on destroy.
- The host is re-checked between launches, and is `@Volatile`.
- Request-history updates are serialized.

An independent review of the branch found the issues fixed in `c5cbaa6` and `199e725`.

IoT permissions added afterwards:
- `BluetoothAdvertise` (`0136be7`): Android API 31+ `BLUETOOTH_ADVERTISE`, auto-granted below; on iOS it shares the Core Bluetooth authorization.
- `NearbyWifiDevices` (`35647de`): Android only. API 33+ `NEARBY_WIFI_DEVICES`; below that `ACCESS_FINE_LOCATION`.
- `LocalNetwork` (`9bdb8c3`): Android only. `ACCESS_LOCAL_NETWORK` is requested only on Android 17+ when the app targets SDK 37+; otherwise it reports Granted (implicit grant) and needs no manifest entry. Unsupported on iOS, which has no API to read local network authorization. The Android sample omits it while it targets SDK 36.

Decided afterwards: BLE scanning below API 31 now requires location permission. `BluetoothScan` maps to `ACCESS_FINE_LOCATION` on API 29–30 and `ACCESS_COARSE_LOCATION` on 26–28, and `Bluetooth` includes it there.

## 3. Left to do

### Needs your decision first
1. **Publishing** (deferred):
   - Choose Maven Central, an internal repository, or source-only.
   - Then add `maven-publish` and signing, `explicitApi()`, the binary-compatibility validator, and an XCFramework/SPM if needed.
   - Move off the alpha/beta dependencies: material3 `1.11.0-alpha07` and lifecycle `2.11.0-beta01`.
2. **SDK**: compileSdk 37 still depends on `android.suppressUnsupportedCompileSdk`. targetSdk stays at 36 until you approve 37.

### Work that can proceed
- **Remaining device coverage**: an Android 12 or older device (Bluetooth location prompt, Nearby Wi-Fi fallback), an Android 17 device once the sample targets SDK 37 (LocalNetwork), and on iOS "Allow Once", Location Services off, and the limited Photos/Contacts pickers.
- **Android device tests in the repo**: the Android pass was driven by hand through adb; an instrumented UI Automator suite like the iOS one would make it repeatable.
- **iOS unit tests**: `iosTest` is empty; the shared tests already run on the simulator in CI.
- **Android engine tests** with Robolectric: the controller and the store. The decision logic is already covered by `AndroidStatusRulesTest`.
- **Compose tests**: none yet; they need the compose ui-test dependency.
- **Compose iOS sample**: the Compose layer has never run on iOS.
- **iOS precise location**: optionally request temporary full accuracy for `LocationFine`. That would need the `NSLocationTemporaryUsageDescriptionDictionary` Info.plist key.

### Housekeeping (for you)
- Template modules not in the build: `androidApp/`, `sharedLogic/`, `sharedUI/`, `iosApp/`. The `iosApp` Xcode project cannot build because it references `:sharedLogic`. I have not deleted them.
- sample-android sources live in `.../sample/` but declare package `...sample.android`. Moving them is a rename, left to you.
- Set your team in `sample-ios/Configuration/Config.xcconfig` (or pass `TEAM_ID=` to `xcodebuild`) before building for a device. The device run used your Personal Team on the command line; no team ID is committed.
- No LICENSE or CHANGELOG yet.

## 4. Known limitations and heuristics
- **Android auto-deny**: a result in under 500 ms is treated as a system auto-deny. A very slow device could misclassify an auto-deny as a dismissal, which would report Denied instead of PermanentlyDenied.
- **Android "Ask every time"**: if the user resets a once-denied permission to "Ask every time" in Settings, it reports PermanentlyDenied until the next request.
- **iOS prompt detection**: relies on WillResignActive/DidBecomeActive, with timeouts of 1 s for a prompt to appear, 0.5 s for the status to settle and 2 s for the app to become active.
- **iOS notifications**: the synchronous `check()` can block for up to 500 ms; `request()` and `observe()` do not block.
- **Cancellation**: a request pending when its Activity is destroyed is cancelled (`CancellationException`), not resumed.
- **Old preference keys**: the pre-branch `simpleName` keys are not migrated. Nothing was published, so this only affects local test installs.
