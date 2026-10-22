package com.kmp.permission.internal

import com.kmp.permission.PermissionException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class PermissionValidatorTest {

    @Test
    fun requireSupported_doesNotThrow_forPermissionsSupportedOnCurrentPlatform() {
        PermissionMapper.allPermissions
            .filter { currentPlatform in PermissionMapper.supportedPlatforms(it) }
            .forEach { permission -> PermissionValidator.requireSupported(permission) }
    }

    @Test
    fun requireSupported_throws_forPermissionsUnsupportedOnCurrentPlatform() {
        PermissionMapper.allPermissions
            .filterNot { currentPlatform in PermissionMapper.supportedPlatforms(it) }
            .forEach { permission ->
                assertFailsWith<PermissionException.UnsupportedPermission> {
                    PermissionValidator.requireSupported(permission)
                }
            }
    }

    @Test
    fun requireNotEmpty_throws_forEmptyCollection() {
        assertFailsWith<IllegalArgumentException> {
            PermissionValidator.requireNotEmpty(emptyList())
        }
    }
}
