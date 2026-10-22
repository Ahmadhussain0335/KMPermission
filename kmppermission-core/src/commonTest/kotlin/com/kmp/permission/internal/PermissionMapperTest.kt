package com.kmp.permission.internal

import kotlin.test.Test
import kotlin.test.assertTrue

class PermissionMapperTest {

    @Test
    fun everyPermissionSupportedOnAndroid_hasAtLeastOneManifestEntry() {
        PermissionMapper.allPermissions
            .filter { Platform.Android in PermissionMapper.supportedPlatforms(it) }
            .forEach { permission ->
                assertTrue(
                    PermissionMapper.androidManifestPermissions(permission).isNotEmpty(),
                    "$permission is marked as Android-supported but has no manifest permission mapped",
                )
            }
    }

    @Test
    fun everyPermissionUnsupportedOnAndroid_hasNoManifestEntry() {
        PermissionMapper.allPermissions
            .filterNot { Platform.Android in PermissionMapper.supportedPlatforms(it) }
            .forEach { permission ->
                assertTrue(
                    PermissionMapper.androidManifestPermissions(permission).isEmpty(),
                    "$permission is marked as unsupported on Android but has a manifest permission mapped",
                )
            }
    }

    @Test
    fun everyPermissionUnsupportedOnIos_hasNoInfoPlistKey() {
        PermissionMapper.allPermissions
            .filterNot { Platform.Ios in PermissionMapper.supportedPlatforms(it) }
            .forEach { permission ->
                assertTrue(
                    PermissionMapper.iosInfoPlistKeys(permission).isEmpty(),
                    "$permission is marked as unsupported on iOS but has an Info.plist key mapped",
                )
            }
    }
}
