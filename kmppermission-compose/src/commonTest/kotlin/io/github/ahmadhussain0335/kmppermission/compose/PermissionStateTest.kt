package io.github.ahmadhussain0335.kmppermission.compose

import io.github.ahmadhussain0335.kmppermission.Permission
import io.github.ahmadhussain0335.kmppermission.PermissionException
import io.github.ahmadhussain0335.kmppermission.PermissionStatus
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

class PermissionStateTest {

    private val manager = FakePermissionManager()

    @Test
    fun initialStatus_isTheCurrentStatus() = runTest {
        manager.setStatus(Permission.Camera, PermissionStatus.Granted)
        lateinit var state: PermissionState

        withComposition({ state = rememberPermissionState(Permission.Camera, manager) }) {
            assertEquals(PermissionStatus.Granted, state.status)
            assertNull(state.error)
        }
    }

    @Test
    fun status_reflectsTheRequestResult() = runTest {
        manager.requestOutcomes[Permission.Camera] = PermissionStatus.Granted
        lateinit var state: PermissionState

        withComposition({ state = rememberPermissionState(Permission.Camera, manager) }) {
            assertEquals(PermissionStatus.NotDetermined, state.status)

            state.launchPermissionRequest()
            advanceUntilIdle()

            assertEquals(PermissionStatus.Granted, state.status)
            assertEquals(listOf(setOf<Permission>(Permission.Camera)), manager.requested)
        }
    }

    @Test
    fun status_followsChangesFromObserve() = runTest {
        lateinit var state: PermissionState

        withComposition({ state = rememberPermissionState(Permission.Camera, manager) }) {
            manager.setStatus(Permission.Camera, PermissionStatus.PermanentlyDenied)
            advanceUntilIdle()
            assertEquals(PermissionStatus.PermanentlyDenied, state.status)

            manager.setStatus(Permission.Camera, PermissionStatus.Granted)
            advanceUntilIdle()
            assertEquals(PermissionStatus.Granted, state.status)
        }
    }

    @Test
    fun configurationError_isReportedWithoutCrashing() = runTest {
        val failure = PermissionException.ManifestPermissionMissing(Permission.Camera, listOf("android.permission.CAMERA"))
        manager.failures[Permission.Camera] = failure
        lateinit var state: PermissionState

        withComposition({ state = rememberPermissionState(Permission.Camera, manager) }) {
            advanceUntilIdle()
            assertSame(failure, state.error)
            assertEquals(PermissionStatus.NotDetermined, state.status)

            state.launchPermissionRequest()
            advanceUntilIdle()
            assertSame(failure, state.error)
        }
    }

    @Test
    fun error_clearsAfterASuccessfulRequest() = runTest {
        manager.failures[Permission.Camera] = PermissionException.UnsupportedPermission(Permission.Camera, "Test")
        manager.requestOutcomes[Permission.Camera] = PermissionStatus.Granted
        lateinit var state: PermissionState

        withComposition({ state = rememberPermissionState(Permission.Camera, manager) }) {
            advanceUntilIdle()
            assertIs<PermissionException.UnsupportedPermission>(state.error)

            manager.failures.clear()
            state.launchPermissionRequest()
            advanceUntilIdle()
            assertNull(state.error)
        }
    }
}
