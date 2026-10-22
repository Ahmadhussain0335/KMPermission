# KMPermission: Project Status Report

Date: 2026-10-22. I rebuilt this from the files alone. The directory is not a git repo, the project memory directory is empty, no plan file in `~/.claude/plans/` mentions KMPermission, and there is no `.claude/skills/` or CLAUDE.md in the project. Anything marked **(inferred)** is my reading, not something the code states. Everything else was checked in the source.

I re-checked three of the bugs below directly in the source: risk 1 (the Bluetooth `pendingContinuation` is overwritten), risk 3 (Calendar maps only to `NSCalendarsUsageDescription`) and risk 4 (`observe(vararg)` has no `requireSupported`).

---

## 1. Project overview

**Purpose (from KDoc):** a Kotlin Multiplatform library that gives one API to check, request and observe runtime permissions on Android and iOS. A Compose Multiplatform layer sits on top, and there is a sample app for each platform.

**Modules in the build** (`settings.gradle.kts`):

| Module | Type | Notes |
|---|---|---|
| `:kmppermission-core` | KMP library (androidMultiplatformLibrary + iosArm64/iosSimulatorArm64) | namespace `com.kmp.permission`, minSdk 26, static framework `KMPPermissionCore` |
| `:kmppermission-compose` | KMP + Compose Multiplatform library | namespace `com.kmp.permission.compose`, minSdk 26, `api(core)`, framework `KMPPermissionCompose` |
| `:sample-android` | Android app | `com.kmp.permission.sample.android`, minSdk 28, targetSdk 36 |
| `sample-ios/` | Xcode project (not Gradle) | Build phase runs `:kmppermission-core:embedAndSignAppleFrameworkForXcode`, deployment target iOS 18.2, bundle id `com.kmp.permission.sample.ios`, `TEAM_ID` empty |

**Template leftovers not in the build:** `androidApp/`, `sharedLogic/`, `sharedUI/`, `iosApp/`. `iosApp`'s build phase still calls `:sharedLogic:embedAndSignAppleFrameworkForXcode`, which is no longer included, so that Xcode project cannot build. `README.md` is still the stock KMP wizard text.

**Key versions** (`gradle/libs.versions.toml`): Kotlin 2.4.0, AGP 9.1.0, Gradle 9.3.1 (JDK 21 toolchain, Amazon), Compose Multiplatform 1.11.1, material3 1.11.0-alpha07, JetBrains lifecycle 2.11.0-beta01, coroutines 1.11.0, androidx.activity 1.13.0, core-ktx 1.19.0. compileSdk 37 (needs `android.suppressUnsupportedCompileSdk=37.0` in `gradle.properties`), targetSdk 36, JVM target 11.

**Build host:** Intel Mac (`x86_64`, i5-1038NG7).

**Build check (2026-10-22, offline):**
- `:kmppermission-core:testAndroidHostTest :kmppermission-compose:compileAndroidMain :sample-android:assembleDebug`: BUILD SUCCESSFUL. All 26 host tests pass (Mapper 3, AndroidMapper 8, DefaultManager 9, Validator 3, Registry 3). The test task was UP-TO-DATE, so these are cached results from the last run.
- `compileKotlinIosArm64`, `compileKotlinIosSimulatorArm64` (core and compose) and `compileTestKotlinIosSimulatorArm64`: BUILD SUCCESSFUL.
- Not run: iOS framework linking, iOS tests (an arm64 simulator cannot run on this x86_64 host), and the Xcode sample build.

**Timeline from file mtimes:** core API and iOS engine on Jul 14; compose module and samples on Jul 15. The last source edits were Jul 15 16:51–16:58 in `PermissionController.android.kt`, `PermissionManager.android.kt`, `PermissionManagerProvider.android.kt` and the sample-android screens. That last edit looks like the switch to registering the launcher directly on `activityResultRegistry` so Compose call sites work after STARTED (inferred). Only Xcode user-state files have changed since (Jul 29, Oct 22).

**Milestones (inferred from KDoc):** M1 public API, M2 mapper, M3 `DefaultPermissionManager`/registry, M4 Android engine, M5 iOS engine, then the Compose layer and samples. All of these exist on disk. No written plan or later milestones were found.

---

## 2. Done

### Common API (`kmppermission-core/src/commonMain/kotlin/com/kmp/permission/`)
- `Permission.kt`: sealed class with 16 cases: Camera, Microphone, Bluetooth, BluetoothScan, BluetoothConnect, LocationFine, LocationCoarse, LocationBackground, Notifications, Contacts, Calendar, Storage, Photos, Nfc, Motion, SpeechRecognition.
- `PermissionStatus.kt`: Granted, Denied, PermanentlyDenied, Restricted, NotDetermined. `PermissionResult.kt` is a typealias of it.
- `PermissionGroup.kt`.
- `PermissionException.kt`: ManifestPermissionMissing, InfoPlistUsageDescriptionMissing, UnsupportedPermission, ManagerNotAttached.
- `PermissionManager.kt`: suspend and callback `request` (single and batch), `check`, `areGranted`, `openSettings`. `PermissionObserver.kt`: `observe` returns a Flow (single and batch).
- `internal/`:
  - `DefaultPermissionManager.kt`, the platform-agnostic orchestrator.
  - `PermissionEngine.kt`, an interface so the manager can be tested with fakes.
  - `PermissionController.kt`, the expect class.
  - `PermissionRegistry.kt`, a StateFlow cache.
  - `PermissionMapper.kt`, which maps each permission to its platforms, manifest strings and Info.plist keys.
  - `PermissionValidator.kt`, `PermissionFactory.kt`, `Platform.kt`.

### Android (`kmppermission-core/src/androidMain/...`)
- `PermissionManager.android.kt`: public factory `PermissionManager(activity: ComponentActivity, scope = activity.lifecycleScope)`.
- `internal/PermissionController.android.kt`: Activity Result `RequestMultiplePermissions`, registered directly on `activityResultRegistry`. A Mutex serializes requests, and the host is detached on `ON_DESTROY`. `openSettings` uses `ACTION_APPLICATION_DETAILS_SETTINGS`.
- `internal/AndroidPermissionMapper.kt`: SDK gating:
  - Photos uses READ_MEDIA_* on API 33+ and falls back to READ_EXTERNAL_STORAGE below that.
  - BT Scan/Connect are runtime only on API 31+.
  - Notifications is runtime only on API 33+.
  - Legacy Bluetooth is always auto-granted.
- `internal/PermissionStatusResolver.kt`: works out Denied vs PermanentlyDenied from `shouldShowRequestPermissionRationale` plus a persisted "was ever requested" flag.
- `internal/RequestedPermissionsStore.kt`: stores that flag in SharedPreferences `com.kmp.permission.requested_permissions`.
- `internal/ManifestPermissionChecker.kt`: throws `ManifestPermissionMissing` if `<uses-permission>` is absent.
- Supported on Android: all cases except Motion and SpeechRecognition, which are rejected by the validator.

### iOS (`kmppermission-core/src/iosMain/...`)
- `PermissionManager.ios.kt`: `PermissionManager(scope = MainScope())`, plus a no-arg overload for Swift.
- `internal/PermissionController.ios.kt`: dispatches to one handler per framework and checks Info.plist first (`InfoPlistChecker.kt`). `openSettings` uses `UIApplicationOpenSettingsURLString`.
- Handlers:

| File | Covers |
|---|---|
| `AvMediaPermissionHandler.kt` | Camera, Microphone |
| `PhotosPermissionHandler.kt` | Photos (Limited is reported as Granted) |
| `BluetoothPermissionHandler.kt` | Bluetooth, Scan and Connect, all sharing CBManager authorization |
| `LocationPermissionHandler.kt` | Fine, Coarse and Background over one shared CLLocationManager |
| `NotificationsPermissionHandler.kt` | Notifications |
| `ContactsPermissionHandler.kt` | Contacts (Limited is reported as Granted) |
| `CalendarPermissionHandler.kt` | Calendar (iOS 17 full-access path; writeOnly is reported as Granted) |
| `MotionPermissionHandler.kt` | Motion |
| `SpeechRecognitionPermissionHandler.kt` | SpeechRecognition |

- Supported on iOS: everything except Storage and Nfc.
- iOS denial always maps to PermanentlyDenied, by design (see `IosPermissionHandler.kt` KDoc).

### Compose (`kmppermission-compose/src/commonMain/kotlin/com/kmp/permission/compose/`)
- `PermissionState.kt` (`rememberPermissionState`), `MultiplePermissionsState.kt` (`rememberMultiplePermissions`), `PermissionLauncher.kt` (`rememberPermissionLauncher`), `PermissionEffect.kt`, `PermissionButton.kt`.
- `PermissionManagerProvider.kt` declares an expect `rememberPermissionManager`. The Android actual shares one manager per Activity through a WeakHashMap; the iOS actual is scoped to the composition.

### Samples
- `sample-android/`: a Compose screen with 10 permission rows (worst-status badge, Request button) and an "Open App Settings" button. The debug APK builds.
- `sample-ios/`: a SwiftUI list over `KMPPermissionCore` with 9 rows and Info.plist usage strings for those rows. It does not use the Compose module.

### Tests (all in `kmppermission-core`)
- `src/commonTest/.../internal/`: `DefaultPermissionManagerTest.kt` (9), `PermissionMapperTest.kt` (3), `PermissionRegistryTest.kt` (3), `PermissionValidatorTest.kt` (3).
- `src/androidHostTest/.../internal/AndroidPermissionMapperTest.kt` (8).

---

## 3. In progress / partial

- **"Changes made in Settings are observed" is promised but not implemented.** The promise is in `PermissionObserver.kt:13-16` and `PermissionState.kt:20-22`. The registry only updates on `check`, `request` or collection start (`DefaultPermissionManager.kt:71,78`). Nothing re-checks on Android `ON_RESUME` or iOS `didBecomeActive`, so a grant or revoke in Settings never reaches collectors.
- **iOS precise vs approximate location is not handled.** There is an explicit NOTE at `LocationPermissionHandler.kt:66-69`: `CLAccuracyAuthorization` is ignored, so LocationFine reports Granted even when only approximate location is allowed.
- **Stale milestone KDoc** refers to future work that is already done: `PermissionController.kt:8-9`, `PermissionFactory.kt:9-10`, `PermissionMapper.kt:10`, `PermissionManager.kt:13-14`.
- **Empty test source sets:** `kmppermission-core/src/iosTest/kotlin/com/kmp` (no files), and `kmppermission-compose/src/{commonTest,androidHostTest,iosTest}/kotlin` (empty).
- **The Compose layer has never run on iOS.** There is no Compose iOS sample or `ComposeUIViewController` host (inferred to be untested).
- **Samples don't cover everything.** Neither sample exercises Nfc, Motion or SpeechRecognition. The iOS Info.plist lacks `NSMotionUsageDescription`, `NSSpeechRecognitionUsageDescription` and `NSCalendarsFullAccessUsageDescription`.
- **Storage KDoc and code disagree.** `Permission.kt:55` says "scoped storage otherwise", but `PermissionMapper.kt:55` always maps to READ_EXTERNAL_STORAGE, and nothing gates it in `AndroidPermissionMapper.kt`.
- No TODO, FIXME, `NotImplementedError` or missing actuals were found. Every expect has an actual for Android and iOS.

---

## 4. Left to do

**Functional gaps**
- Re-checking on foreground or resume, so Settings changes reach observers (see section 3).
- Partial grants have no distinct status: Android 14+ `READ_MEDIA_VISUAL_USER_SELECTED`, iOS Photos/Contacts Limited, Calendar writeOnly, approximate location. Adding one is an API decision (see questions).
- Android Storage on API 33+ is not handled at all.
- Android background location:
  - API < 29 gating.
  - Enforcing that foreground is granted before asking for background.
  - API 30+ behavior, where the system sends the user to Settings.
- Android 12+: Fine must be requested together with Coarse, and an approximate-only grant needs handling.
- Notifications below API 33 report Granted. They should probably reflect `NotificationManagerCompat.areNotificationsEnabled()`.
- Permissions relevant to IoT gateways are missing (inferred to matter for this domain):
  - BLUETOOTH_ADVERTISE.
  - NEARBY_WIFI_DEVICES (API 33+).
  - Local network access on Android 17. I believe this is an enforced permission when targeting 37; please check the current docs.
  - iOS Local Network (`NSLocalNetworkUsageDescription` plus `NSBonjourServices`).
  - BLE scanning on API 30 and below, which really depends on ACCESS_FINE_LOCATION. The library reports BluetoothScan as auto-Granted there.

**Tests**
- Android engine: resolver, controller, store and manifest checker. These need Robolectric or extracted fakes.
- iOS handlers: at least the mapping functions.
- Compose state holders.
- Concurrency tests (concurrent requests) and config-change tests.

**Docs**
- Replace the template README with library docs: setup, required manifest/Info.plist entries per permission, the Denied vs PermanentlyDenied semantics, and Swift usage.
- `docs/` is empty. No CHANGELOG or LICENSE.

**Publishing**
- None configured: no `maven-publish`, signing, group/version or POM, and no XCFramework or SPM output.
- No `explicitApi()` and no binary-compatibility validator.
- No consumer ProGuard rules (`consumer-rules.pro` is missing).

**CI**
- No CI config (GitHub, GitLab or Xcode Cloud).

**Housekeeping**
- The template modules (`androidApp/`, `sharedLogic/`, `sharedUI/`, `iosApp/`) are candidates for removal. You would remove them yourself.
- Put the project under git.

---

## 5. Risks and issues found

### Correctness bugs (confirmed in code; runtime effect partly inferred)

**1. iOS requests can hang forever when made concurrently.**
- `BluetoothPermissionHandler.kt:32-46` and `LocationPermissionHandler.kt:28-51` each keep a single `pendingContinuation`, and a second request overwrites it.
- The iOS sample does exactly this. `ContentView.swift:72-81` fires a separate `request` per permission, so the three Bluetooth requests and the Fine+Coarse pair race.
- No `invokeOnCancellation` cleanup either.

**2. iOS background location can hang.**
- `LocationPermissionHandler.kt:40-46,99-104`: `requestAlways()` suspends whenever the status is not AuthorizedAlways.
- If the status is Denied or Restricted, or the one-time "upgrade to Always" prompt has already been used or the user keeps "While Using", iOS fires no authorization callback. The coroutine never resumes (inferred from CoreLocation behavior).
- Related risk (inferred): iOS 14+ calls `locationManagerDidChangeAuthorization` when the manager is created, so a request that triggers the lazy init can resume early. Delegate callbacks also need the manager to be created on a thread with a run loop (normally main).

**3. iOS Calendar on iOS 17+.**
- `CalendarPermissionHandler.kt:28-29` calls `requestFullAccessToEvents`, but `PermissionMapper.kt:75` and `InfoPlistChecker.kt:14` only check `NSCalendarsUsageDescription`.
- iOS 17+ requires `NSCalendarsFullAccessUsageDescription`. Without it the request fails or the app is terminated (please check). The sample's deployment target is 18.2, so it always takes this path.
- `InfoPlistChecker` also only checks the first key (`firstOrNull`). Background location really needs both the WhenInUse and Always keys.

**4. Android `observe(vararg)` skips validation.**
- `DefaultPermissionManager.kt:74-79` does not call `requireSupported`.
- On Android, Motion or SpeechRecognition then hits `PermissionStatusResolver.kt:23-27`, where the manifest list is empty, so `all {}` is true and the result is **Granted**.

**5. Neither platform switches to the main thread.**
- Android: `PermissionController.android.kt:102-108` calls `launcher.launch` on the caller's thread. A suspend `request` from `Dispatchers.IO` or Default launches off main.
- iOS: `CBCentralManager(delegate, queue = null)` (`BluetoothPermissionHandler.kt:40`), `CLLocationManager` creation, and `UIApplication.openURL` (`PermissionController.ios.kt:44-47`) all assume main.

**6. Android loses the result on config change or process death.**
- The launcher key embeds a per-process counter (`PermissionController.android.kt:50-51`). A recreated Activity registers a different key, so the pending result is dropped.
- Suspend callers outside `lifecycleScope`, such as a ViewModel scope, then never resume (inferred). The launcher is also never `unregister()`ed.

**7. Android "permanently denied" detection has false positives.**
- `RequestedPermissionsStore` marks a permission as requested before the prompt (`PermissionController.android.kt:88`).
- Dismissing the dialog, auto-revoke of unused-app permissions, and restored backups then show as PermanentlyDenied while a prompt would still appear (inferred).
- The sample has `android:allowBackup="true"` (`sample-android/src/main/AndroidManifest.xml:21`) with no data-extraction rules, so the prefs file is backed up and restored to new devices.

**8. R8 could make the prefs keys unstable.**
- `RequestedPermissionsStore.kt:23` keys on `permission::class.simpleName`.
- Under R8 in a consumer's release build, class names may be obfuscated and can change between releases, which silently resets stored state (inferred). Use stable string IDs, or ship consumer keep rules.

**9. Legacy Bluetooth on Android 12+.**
- `ManifestPermissionChecker.kt:37` reads `requestedPermissions`.
- Apps that correctly declare `BLUETOOTH`/`BLUETOOTH_ADMIN` with `maxSdkVersion="30"` will probably not have them listed on API 31+ (inferred), so `check(Permission.Bluetooth)` throws `ManifestPermissionMissing`.
- `Permission.Bluetooth` also reports Granted on 31+ even though scanning and connecting are not, which is misleading.

**10. Storage on API 33+.**
- READ_EXTERNAL_STORAGE is never granted at targetSdk 33+, so `Permission.Storage` shows PermanentlyDenied without a prompt (inferred from platform behavior).

**11. Background location below API 29.**
- `ACCESS_BACKGROUND_LOCATION` is required and requested on API 26–28, where it does not exist (`AndroidPermissionMapper.kt:39-45` has no gating). It resolves as not granted.

**12. The iOS notifications check blocks the calling thread.**
- `NotificationsPermissionHandler.kt:24-30` wraps `runBlocking` around an async API inside synchronous `status()`. That is usually the main thread, and Compose calls `check()` during composition (`PermissionState.kt:39`).
- Line 27 force-unwraps `settings!!`.

**13. Compose error handling.**
- `ManifestPermissionMissing`, `InfoPlistUsageDescriptionMissing` and `ManagerNotAttached` thrown inside `scope.launch` (`PermissionState.kt:44`, `MultiplePermissionsState.kt:35`, `PermissionLauncher.kt:32`) are uncaught and crash the app.
- `PermissionState.kt:39` does a synchronous `check()`, which on Android means `getPackageInfo` IPC on every call, during composition.
- `MultiplePermissionsState.kt:43` calls `rememberPermissionState` inside a `map` loop, which breaks if the vararg count changes.

**14. Multiple managers per Activity on Android.**
- `SampleActivity.kt:17` creates its own manager, separate from the Compose cache. Two launchers and two mutexes mean concurrent dialogs can collide.

**15. Minor issues.**
- iOS `CBCentralManager` is created without `CBCentralManagerOptionShowPowerAlertKey = false`, so a "Turn on Bluetooth" system alert may appear.
- `Permission.kt:11-13` claims adding sealed subclasses is non-breaking. It still breaks consumers' exhaustive `when` without `else`.
- sample-android sources live in `.../sample/` but declare package `com.kmp.permission.sample.android`.

### Build and platform risks
- **Intel host.** Kotlin/Native `macos_x64` hosts are deprecated (warning printed in this build). There is no `iosX64` target, so the iOS sample cannot run on a simulator on this Mac, only on a device.
- **Pre-release dependencies.** compileSdk 37 works only by suppressing the AGP 9.1.0 check. material3 is `1.11.0-alpha07` and lifecycle is `2.11.0-beta01`, and a published library would pass these pre-releases on to consumers. Compose depends on material3 only for `PermissionButton`.
- **Android 16 (targetSdk 36).** The sample already calls `enableEdgeToEdge`. Large-screen orientation and resizability restrictions are ignored on API 36, so check sample layouts. Body-sensor permissions move to health permissions (not in scope today).
- **Android 17 (API 37).** Local network access permission for IoT use cases, and any further restrictions on permission behavior. I can't confirm the exact Android 17 rules; check them before raising targetSdk. Targeting 37 needs your approval.
- **Google Play policy for the sample manifest.**
  - ACCESS_BACKGROUND_LOCATION and READ_MEDIA_IMAGES/VIDEO need Play policy declarations (photo-picker requirement).
  - BLUETOOTH/BLUETOOTH_ADMIN lack `maxSdkVersion="30"`.
  - BLUETOOTH_SCAN lacks `neverForLocation`.
  - READ_EXTERNAL_STORAGE lacks `maxSdkVersion="32"` (`sample-android/src/main/AndroidManifest.xml:6-8,16`).
- **iOS Info.plist and App Review.**
  - Missing keys: `NSCalendarsFullAccessUsageDescription` (iOS 17+), `NSMotionUsageDescription`, `NSSpeechRecognitionUsageDescription`.
  - Real use of background location needs `UIBackgroundModes: location` plus a justified Always prompt.
  - Usage strings must name the actual feature. Generic "demonstrate" text is fine for a sample but would be rejected in a shipping app.
- **Signing.** `sample-ios` has an empty `DEVELOPMENT_TEAM` (`Config.xcconfig` `TEAM_ID=`). Device builds and Xcode Cloud will need a team and provisioning set.

### Security and compliance
- No hardcoded secrets, tokens or keys were found. No network code, no telemetry, no PII.
- The library stores only boolean "requested" flags in plain SharedPreferences. That data is not sensitive, so plain prefs are fine; it does not need encrypted storage. Backup handling is the issue (item 7).
- Sample `allowBackup="true"` with no `dataExtractionRules` would be flagged by MASVS-STORAGE and backup checks.
- The exported component is only the launcher activity, which is expected.

---

## 6. Open questions for you

1. **Distribution:** Maven Central, an internal Nexus, or source-only? What group ID and coordinates? Do you need an XCFramework or SPM package for iOS consumers?
2. **Partial grants:** should Limited/Approximate/WriteOnly/UserSelected become a new `PermissionStatus` case (an API change) or stay mapped to Granted?
3. **Scope:** should IoT-relevant permissions be added (BLUETOOTH_ADVERTISE, NEARBY_WIFI_DEVICES, Local Network on iOS and Android 17)? Should BluetoothScan on API 30 and below include the location requirement?
4. **Background location UX:** should the library enforce the "foreground first, then background" sequence, or only document it?
5. **Template modules:** can `androidApp/`, `sharedLogic/`, `sharedUI/` and `iosApp/` be removed (by you)?
6. **Roadmap:** was there a written milestone plan beyond M5 and the Compose/sample work? None was found on disk.
7. **SDK and toolchain:** keep compileSdk 37 with the suppression flag, or wait for an AGP that supports it? Raise targetSdk to 37 now or later?
8. **iOS testing:** is an Apple-silicon Mac or CI runner available for iOS simulator tests? Otherwise, should an `iosX64` target be added for this Intel machine?
9. **Version control:** OK to initialize git and commit the current state as a baseline before fixes?

---

## 7. Suggested next steps (by priority)

1. **Baseline:** `git init` and an initial commit, with your go-ahead, so later fixes are reviewable.
2. **Fix hangs and crashes on iOS:**
   - Queue or serialize continuations in the Bluetooth and Location handlers.
   - Make background location resolve without waiting when no prompt will appear.
   - Add `NSCalendarsFullAccessUsageDescription` mapping and check every required plist key.
   - Remove `runBlocking` and `!!` from Notifications.
   - Create CB/CL managers and call `openURL` on main.
3. **Fix Android correctness:**
   - Add `requireSupported` to `observe(vararg)`.
   - Move to the main thread for `launch`.
   - Use stable registration keys or survive Activity recreation.
   - Handle Storage on API 33+, background location below 29 and its ordering, and Fine+Coarse pairing on 31+.
   - Handle legacy BT `maxSdkVersion`.
   - Use stable prefs keys and document backup exclusion.
4. **Settings-change observation:** re-check on Android `ON_RESUME` and iOS `UIApplicationDidBecomeActiveNotification` so the KDoc promise holds.
5. **Tests:**
   - Android engine (Robolectric or fakes around resolver and store).
   - iOS mapping tests.
   - Compose state tests.
   - Regression tests for each bug in steps 2–3.
6. **Compose hardening:** surface developer exceptions instead of crashing in `launch`, avoid blocking `check` during composition, and decide whether `material3` stays a dependency.
7. **Samples:** fix the iOS sample's concurrent per-permission requests, add the missing plist keys, and tidy the sample-android manifest (maxSdkVersion, neverForLocation, backup rules). Optionally add a Compose iOS sample.
8. **Docs and cleanup:** a real README with the per-permission manifest/plist table, and remove the template modules (you).
9. **Publishing:** `explicitApi()`, binary-compatibility validator, consumer ProGuard rules, `maven-publish` plus signing, XCFramework/SPM if needed, and moving off alpha/beta dependencies.
10. **CI:** Android host tests and assemble on every MR; an iOS compile and test job on an arm64 runner (or Xcode Cloud).
11. **Scope extension:** IoT-relevant permissions, once you've answered question 3.

**Key file paths:**
- `kmppermission-core/src/commonMain/kotlin/com/kmp/permission/`
- `kmppermission-core/src/androidMain/kotlin/com/kmp/permission/internal/PermissionController.android.kt`
- `kmppermission-core/src/iosMain/kotlin/com/kmp/permission/internal/LocationPermissionHandler.kt`
- `kmppermission-core/src/iosMain/kotlin/com/kmp/permission/internal/BluetoothPermissionHandler.kt`
- `kmppermission-compose/src/commonMain/kotlin/com/kmp/permission/compose/`
- `sample-android/src/main/AndroidManifest.xml`
- `sample-ios/iosApp/ContentView.swift`
- `sample-ios/iosApp/Info.plist`
- `gradle/libs.versions.toml`
