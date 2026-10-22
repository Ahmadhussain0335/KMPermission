# Changelog

All notable changes to KMPermission. The Maven coordinates (`io.github.ahmadhussain0335:kmppermission-core` and `kmppermission-compose`), the iOS framework name (`KMPPermissionCore`) and the Swift package product (`KMPPermissionCore`) have not changed between versions.

## 0.3.0 (unreleased)

### Breaking changes

- **The Kotlin package is now `io.github.ahmadhussain0335.kmppermission`.** It used to be `com.kmp.permission`, a namespace this project doesn't own; the new one matches the Maven group. The Compose layer moved from `com.kmp.permission.compose` to `io.github.ahmadhussain0335.kmppermission.compose`. Class and function names are unchanged. See [Migrating from 0.2.x](#migrating-from-02x).
- The Android namespaces of the library modules changed to match: `io.github.ahmadhussain0335.kmppermission` and `io.github.ahmadhussain0335.kmppermission.compose`.

### Unchanged on purpose

- **The SharedPreferences file is still named `com.kmp.permission.requested_permissions`.** It holds the request history that tells `Denied` apart from `PermanentlyDenied` on Android. Renaming it would drop that history for existing users, so permissions they had denied would read as never requested. Backup exclusion rules that name this file keep working without changes.
- **Swift code is unaffected.** Swift sees the same type names from the `KMPPermissionCore` framework (for example `PermissionManager_iosKt.PermissionManager()`).

### Migrating from 0.2.x

Replace the package in your imports; nothing else changes:

```diff
-import com.kmp.permission.Permission
-import com.kmp.permission.PermissionManager
-import com.kmp.permission.PermissionStatus
-import com.kmp.permission.compose.rememberPermissionState
+import io.github.ahmadhussain0335.kmppermission.Permission
+import io.github.ahmadhussain0335.kmppermission.PermissionManager
+import io.github.ahmadhussain0335.kmppermission.PermissionStatus
+import io.github.ahmadhussain0335.kmppermission.compose.rememberPermissionState
```

A project-wide find and replace of `com.kmp.permission` with `io.github.ahmadhussain0335.kmppermission` in Kotlin files does it. Don't rewrite the prefs file name in your backup rules: it hasn't changed.

## 0.2.1

- First release with a Swift package (`KMPPermissionCore`) for iOS apps that don't use Kotlin, built from a static XCFramework attached to the GitHub release.
- The Kotlin library code is identical to 0.2.0.

## 0.2.0

- First release on Maven Central.
- Breaking: `PermissionButton` no longer uses material3. It is a style-free clickable area, and `kmppermission-compose` depends only on Compose runtime and foundation.
- Licensed under Apache 2.0. The public API is locked with explicit API mode and Kotlin ABI validation.

## 0.1.0

- First tagged version (GitHub only): 19 permissions for Android and iOS, six statuses including `Limited`, suspend, callback and `Flow` APIs, and the Compose layer. Tested on a Samsung Galaxy A53 (Android 16) and an iPhone 13 (iOS 26.6).
