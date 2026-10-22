# KMPermission: Project Status

Last updated: 2026-10-22. Branch `fix/stability-pass` (19 commits on top of the `main` baseline). Not pushed; there is no remote yet.

## 1. Where things stand

| Area | State |
|---|---|
| Common API | 16 permissions; 6 statuses, including the new `Limited`; check / request / observe / openSettings. |
| Android engine | Done. SDK-gated for API 26–36. |
| iOS engine | Done. Minimum iOS 15. |
| Compose layer | Done. Android verified by the sample; iOS compiles but has never been run. |
| Samples | Android (Compose) and iOS (SwiftUI); both build. |
| Tests | 55 host tests: 13 manager, 7 mapper, 20 Android mapper, 9 Android status rules, 3 registry, 3 validator. All passing. |
| Docs | README rewritten: setup table, usage, platform notes. |
| Publishing / CI | Not started (deferred by decision). |

**Verified on 2026-10-22** (Intel Mac, offline):
- Gradle, clean rebuild (`--rerun-tasks`):
  - All host tests pass.
  - core and compose compile for Android, iosArm64 and iosSimulatorArm64.
  - iOS test sources compile.
  - The debug iosArm64 framework links.
  - `sample-android:assembleDebug` builds.
  - `lintDebug` reports 0 errors and 6 warnings.
- Xcode 16.3: `sample-ios` builds for a generic iOS device with signing disabled.
- Android device (SM-A536E, Android 16): the sample installs and launches without a crash. The UI was not exercised because the phone was locked.

**Not verified:**
- No iOS runtime behaviour has been exercised. This machine cannot run an arm64 simulator, and no iPhone was available.
- No permission dialog flow has been driven on a device.
- The iOS prompt-detection logic and the Android 500 ms auto-deny threshold therefore still need a manual pass on real hardware (see section 4).

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

Decided afterwards: BLE scanning below API 31 now requires location permission. `BluetoothScan` maps to `ACCESS_FINE_LOCATION` on API 29–30 and `ACCESS_COARSE_LOCATION` on 26–28, and `Bluetooth` includes it there.

## 3. Left to do

### Needs your decision first
1. **IoT permissions** (deferred): BLUETOOTH_ADVERTISE, NEARBY_WIFI_DEVICES, local network access (iOS `NSLocalNetworkUsageDescription`, and Android 17, which needs checking against the current docs).
2. **Publishing** (deferred):
   - Choose Maven Central, an internal repository, or source-only.
   - Then add `maven-publish` and signing, `explicitApi()`, the binary-compatibility validator, and an XCFramework/SPM if needed.
   - Move off the alpha/beta dependencies: material3 `1.11.0-alpha07` and lifecycle `2.11.0-beta01`.
3. **CI**: GitHub Actions or GitLab CI depends on where the repo will live. Plan: host tests and assemble on every MR, plus an iOS job on an arm64 runner or Xcode Cloud.
4. **SDK**: compileSdk 37 still depends on `android.suppressUnsupportedCompileSdk`. targetSdk stays at 36 until you approve 37.

### Work that can proceed
- **Manual device test pass** on real devices:
  - iOS: the location When In Use → Always sequence, including "Allow Once", "Keep While Using" and Location Services off; Bluetooth; Notifications.
  - Android: a dismissed dialog, "Don't allow" twice, background location on API 30+, selected photos on API 34+, rotation during a dialog, Bluetooth on an API 30 or older device (location prompt).
- **iOS tests**: `iosTest` is empty, and they need an Apple-silicon Mac.
- **Android engine tests** with Robolectric: the controller and the store. The decision logic is already covered by `AndroidStatusRulesTest`.
- **Compose tests**: none yet; they need the compose ui-test dependency.
- **Compose iOS sample**: the Compose layer has never run on iOS.
- **iOS precise location**: optionally request temporary full accuracy for `LocationFine`. That would need the `NSLocationTemporaryUsageDescriptionDictionary` Info.plist key.

### Housekeeping (for you)
- Template modules not in the build: `androidApp/`, `sharedLogic/`, `sharedUI/`, `iosApp/`. The `iosApp` Xcode project cannot build because it references `:sharedLogic`. I have not deleted them.
- sample-android sources live in `.../sample/` but declare package `...sample.android`. Moving them is a rename, left to you.
- Set your team in `sample-ios/Configuration/Config.xcconfig` before building for a device or Xcode Cloud.
- No LICENSE or CHANGELOG yet.

## 4. Known limitations and heuristics
- **Android auto-deny**: a result in under 500 ms is treated as a system auto-deny. A very slow device could misclassify an auto-deny as a dismissal, which would report Denied instead of PermanentlyDenied.
- **Android "Ask every time"**: if the user resets a once-denied permission to "Ask every time" in Settings, it reports PermanentlyDenied until the next request.
- **iOS prompt detection**: relies on WillResignActive/DidBecomeActive, with timeouts of 1 s for a prompt to appear, 0.5 s for the status to settle and 2 s for the app to become active.
- **iOS notifications**: the synchronous `check()` can block for up to 500 ms; `request()` and `observe()` do not block.
- **Cancellation**: a request pending when its Activity is destroyed is cancelled (`CancellationException`), not resumed.
- **Old preference keys**: the pre-branch `simpleName` keys are not migrated. Nothing was published, so this only affects local test installs.
