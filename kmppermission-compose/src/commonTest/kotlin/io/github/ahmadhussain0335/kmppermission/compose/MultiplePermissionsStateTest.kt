package io.github.ahmadhussain0335.kmppermission.compose

import io.github.ahmadhussain0335.kmppermission.Permission
import io.github.ahmadhussain0335.kmppermission.PermissionException
import io.github.ahmadhussain0335.kmppermission.PermissionStatus
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MultiplePermissionsStateTest {

    private val manager = FakePermissionManager()
    private val camera = Permission.Camera
    private val microphone = Permission.Microphone

    @Test
    fun initialStatuses_areTheCurrentStatuses() = runTest {
        manager.setStatus(camera, PermissionStatus.Granted)
        lateinit var state: MultiplePermissionsState

        withComposition({ state = rememberMultiplePermissions(listOf(camera, microphone), manager) }) {
            assertEquals(listOf(PermissionStatus.Granted, PermissionStatus.NotDetermined), state.permissions.map { it.status })
            assertTrue(state.anyGranted)
            assertFalse(state.allGranted)
            assertNull(state.error)
        }
    }

    @Test
    fun batchRequest_updatesEveryPermission() = runTest {
        manager.requestOutcomes[camera] = PermissionStatus.Granted
        manager.requestOutcomes[microphone] = PermissionStatus.Granted
        lateinit var state: MultiplePermissionsState

        withComposition({ state = rememberMultiplePermissions(listOf(camera, microphone), manager) }) {
            state.launchMultiplePermissionRequest()
            advanceUntilIdle()

            assertTrue(state.allGranted)
            assertEquals(listOf(setOf(camera, microphone)), manager.requested)
        }
    }

    @Test
    fun limited_doesNotCountAsGranted() = runTest {
        manager.setStatus(camera, PermissionStatus.Limited)
        lateinit var state: MultiplePermissionsState

        withComposition({ state = rememberMultiplePermissions(listOf(camera), manager) }) {
            assertFalse(state.anyGranted)
            assertFalse(state.allGranted)
        }
    }

    @Test
    fun statuses_followChangesFromObserve() = runTest {
        lateinit var state: MultiplePermissionsState

        withComposition({ state = rememberMultiplePermissions(listOf(camera, microphone), manager) }) {
            manager.setStatus(microphone, PermissionStatus.Denied)
            advanceUntilIdle()

            assertEquals(PermissionStatus.Denied, state.permissions[1].status)
        }
    }

    @Test
    fun configurationError_isReportedWithoutCrashing() = runTest {
        val failure = PermissionException.InfoPlistUsageDescriptionMissing(microphone, "NSMicrophoneUsageDescription")
        manager.failures[microphone] = failure
        lateinit var state: MultiplePermissionsState

        withComposition({ state = rememberMultiplePermissions(listOf(camera, microphone), manager) }) {
            advanceUntilIdle()
            assertSame(failure, state.error)

            state.launchMultiplePermissionRequest()
            advanceUntilIdle()
            assertSame(failure, state.error)
            assertEquals(emptyList(), manager.requested)
        }
    }
}
