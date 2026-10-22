package com.kmp.permission.internal

import com.kmp.permission.Permission
import com.kmp.permission.PermissionStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PermissionRegistryTest {

    @Test
    fun statusFlow_emitsNotDetermined_beforeAnyUpdate() = runTest {
        val registry = PermissionRegistry()

        assertEquals(PermissionStatus.NotDetermined, registry.statusFlow(Permission.Camera).first())
    }

    @Test
    fun statusFlow_emitsUpdatedStatus_afterUpdate() = runTest {
        val registry = PermissionRegistry()

        registry.update(Permission.Camera, PermissionStatus.Granted)

        assertEquals(PermissionStatus.Granted, registry.statusFlow(Permission.Camera).first())
    }

    @Test
    fun batchStatusFlow_reflectsEachRequestedPermissionIndependently() = runTest {
        val registry = PermissionRegistry()

        registry.update(Permission.Camera, PermissionStatus.Granted)

        val statuses = registry.statusFlow(setOf(Permission.Camera, Permission.Microphone)).first()

        assertEquals(PermissionStatus.Granted, statuses[Permission.Camera])
        assertEquals(PermissionStatus.NotDetermined, statuses[Permission.Microphone])
    }
}
