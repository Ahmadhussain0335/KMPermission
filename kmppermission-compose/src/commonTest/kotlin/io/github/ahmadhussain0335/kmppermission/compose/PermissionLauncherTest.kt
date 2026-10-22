package io.github.ahmadhussain0335.kmppermission.compose

import io.github.ahmadhussain0335.kmppermission.Permission
import io.github.ahmadhussain0335.kmppermission.PermissionException
import io.github.ahmadhussain0335.kmppermission.PermissionResult
import io.github.ahmadhussain0335.kmppermission.PermissionStatus
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class PermissionLauncherTest {

    private val manager = FakePermissionManager()

    @Test
    fun nothingIsReported_beforeLaunch() = runTest {
        var results: Map<Permission, PermissionResult>? = null

        withComposition({ rememberPermissionLauncher(manager, onError = { throw it }) { results = it } }) {
            advanceUntilIdle()
            assertNull(results)
            assertEquals(emptyList(), manager.requested)
        }
    }

    @Test
    fun launch_reportsEveryResult() = runTest {
        manager.requestOutcomes[Permission.Camera] = PermissionStatus.Granted
        manager.requestOutcomes[Permission.Microphone] = PermissionStatus.PermanentlyDenied
        var results: Map<Permission, PermissionResult>? = null
        lateinit var launcher: PermissionLauncher

        withComposition({ launcher = rememberPermissionLauncher(manager, onError = { throw it }) { results = it } }) {
            launcher.launch(Permission.Camera, Permission.Microphone)
            advanceUntilIdle()

            assertEquals(
                mapOf<Permission, PermissionResult>(
                    Permission.Camera to PermissionStatus.Granted,
                    Permission.Microphone to PermissionStatus.PermanentlyDenied,
                ),
                results,
            )
        }
    }

    @Test
    fun configurationError_goesToOnErrorWithoutCrashing() = runTest {
        val failure = PermissionException.UnsupportedPermission(Permission.Nfc, "iOS")
        manager.failures[Permission.Nfc] = failure
        var error: PermissionException? = null
        var results: Map<Permission, PermissionResult>? = null
        lateinit var launcher: PermissionLauncher

        withComposition({ launcher = rememberPermissionLauncher(manager, onError = { error = it }) { results = it } }) {
            launcher.launch(Permission.Nfc)
            advanceUntilIdle()

            assertSame(failure, error)
            assertNull(results)
        }
    }
}
