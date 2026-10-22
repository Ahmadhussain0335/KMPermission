package com.kmp.permission.internal

import com.kmp.permission.Permission
import com.kmp.permission.PermissionException
import com.kmp.permission.PermissionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DefaultPermissionManagerTest {

    private class FakePermissionEngine(
        private val statuses: MutableMap<Permission, PermissionStatus> = mutableMapOf(),
    ) : PermissionEngine {
        var openSettingsCallCount = 0
            private set

        fun seed(permission: Permission, status: PermissionStatus) {
            statuses[permission] = status
        }

        override fun status(permission: Permission): PermissionStatus =
            statuses[permission] ?: PermissionStatus.NotDetermined

        override suspend fun request(permissions: Set<Permission>): Map<Permission, PermissionStatus> =
            permissions.associateWith { statuses[it] ?: PermissionStatus.NotDetermined }

        override fun openSettings() {
            openSettingsCallCount++
        }

        private val foreground = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        override val foregroundEvents: Flow<Unit> = foreground

        fun bringToForeground() {
            foreground.tryEmit(Unit)
        }
    }

    private fun unsupportedPermission(): Permission =
        PermissionMapper.allPermissions.first { currentPlatform !in PermissionMapper.supportedPlatforms(it) }

    @Test
    fun request_singlePermission_returnsAndCachesEngineResult() = runTest {
        val engine = FakePermissionEngine().apply { seed(Permission.Camera, PermissionStatus.Granted) }
        val registry = PermissionRegistry()
        val manager = DefaultPermissionManager(engine, registry, this)

        assertEquals(PermissionStatus.Granted, manager.request(Permission.Camera))
        assertEquals(PermissionStatus.Granted, registry.statusFlow(Permission.Camera).first())
    }

    @Test
    fun request_batch_returnsEveryResult() = runTest {
        val engine = FakePermissionEngine().apply {
            seed(Permission.Camera, PermissionStatus.Granted)
            seed(Permission.Microphone, PermissionStatus.Denied)
        }
        val manager = DefaultPermissionManager(engine, PermissionRegistry(), this)

        val results = manager.request(Permission.Camera, Permission.Microphone)

        assertEquals(PermissionStatus.Granted, results[Permission.Camera])
        assertEquals(PermissionStatus.Denied, results[Permission.Microphone])
    }

    @Test
    fun request_callbackVariant_invokesCallbackWithResult() = runTest {
        val engine = FakePermissionEngine().apply { seed(Permission.Camera, PermissionStatus.Granted) }
        val manager = DefaultPermissionManager(engine, PermissionRegistry(), this)

        var received: PermissionStatus? = null
        manager.request(Permission.Camera) { received = it }
        advanceUntilIdle()

        assertEquals(PermissionStatus.Granted, received)
    }

    @Test
    fun request_batchCallbackVariant_invokesCallbackWithEveryResult() = runTest {
        val engine = FakePermissionEngine().apply {
            seed(Permission.Camera, PermissionStatus.Granted)
            seed(Permission.Microphone, PermissionStatus.Denied)
        }
        val manager = DefaultPermissionManager(engine, PermissionRegistry(), this)

        var received: Map<Permission, PermissionStatus>? = null
        manager.request(listOf(Permission.Camera, Permission.Microphone)) { received = it }
        advanceUntilIdle()

        assertEquals(PermissionStatus.Granted, received?.get(Permission.Camera))
        assertEquals(PermissionStatus.Denied, received?.get(Permission.Microphone))
    }

    @Test
    fun check_unsupportedPermission_throws() {
        val manager = DefaultPermissionManager(
            FakePermissionEngine(),
            PermissionRegistry(),
            CoroutineScope(Dispatchers.Unconfined),
        )

        assertFailsWith<PermissionException.UnsupportedPermission> {
            manager.check(unsupportedPermission())
        }
    }

    @Test
    fun areGranted_isFalse_whenAnyPermissionNotGranted() {
        val engine = FakePermissionEngine().apply {
            seed(Permission.Camera, PermissionStatus.Granted)
            seed(Permission.Microphone, PermissionStatus.Denied)
        }
        val manager = DefaultPermissionManager(engine, PermissionRegistry(), CoroutineScope(Dispatchers.Unconfined))

        assertFalse(manager.areGranted(Permission.Camera, Permission.Microphone))
    }

    @Test
    fun areGranted_isTrue_whenEveryPermissionGranted() {
        val engine = FakePermissionEngine().apply {
            seed(Permission.Camera, PermissionStatus.Granted)
            seed(Permission.Microphone, PermissionStatus.Granted)
        }
        val manager = DefaultPermissionManager(engine, PermissionRegistry(), CoroutineScope(Dispatchers.Unconfined))

        assertTrue(manager.areGranted(Permission.Camera, Permission.Microphone))
    }

    @Test
    fun openSettings_delegatesToEngine() {
        val engine = FakePermissionEngine()
        val manager = DefaultPermissionManager(engine, PermissionRegistry(), CoroutineScope(Dispatchers.Unconfined))

        manager.openSettings()

        assertEquals(1, engine.openSettingsCallCount)
    }

    @Test
    fun observe_emitsCurrentStatusOnCollection() = runTest {
        val engine = FakePermissionEngine().apply { seed(Permission.Camera, PermissionStatus.Granted) }
        val manager = DefaultPermissionManager(engine, PermissionRegistry(), this)

        assertEquals(PermissionStatus.Granted, manager.observe(Permission.Camera).first())
    }

    @Test
    fun observe_batch_unsupportedPermission_throws() {
        val manager = DefaultPermissionManager(
            FakePermissionEngine(),
            PermissionRegistry(),
            CoroutineScope(Dispatchers.Unconfined),
        )

        assertFailsWith<PermissionException.UnsupportedPermission> {
            manager.observe(Permission.Camera, unsupportedPermission())
        }
    }

    @Test
    fun observe_reemitsStatusChangedInSettings_whenAppReturnsToForeground() = runTest {
        val engine = FakePermissionEngine().apply { seed(Permission.Camera, PermissionStatus.Denied) }
        val manager = DefaultPermissionManager(engine, PermissionRegistry(), this)
        val emissions = mutableListOf<PermissionStatus>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { manager.observe(Permission.Camera).toList(emissions) }

        engine.seed(Permission.Camera, PermissionStatus.Granted)
        engine.bringToForeground()

        assertEquals(listOf(PermissionStatus.Denied, PermissionStatus.Granted), emissions)
        collector.cancel()
    }

    @Test
    fun observe_batch_reemitsOnForeground() = runTest {
        val engine = FakePermissionEngine().apply {
            seed(Permission.Camera, PermissionStatus.Granted)
            seed(Permission.Microphone, PermissionStatus.NotDetermined)
        }
        val manager = DefaultPermissionManager(engine, PermissionRegistry(), this)
        val emissions = mutableListOf<Map<Permission, PermissionStatus>>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            manager.observe(Permission.Camera, Permission.Microphone).toList(emissions)
        }

        engine.seed(Permission.Microphone, PermissionStatus.PermanentlyDenied)
        engine.bringToForeground()

        assertEquals(PermissionStatus.PermanentlyDenied, emissions.last()[Permission.Microphone])
        collector.cancel()
    }

    @Test
    fun observe_doesNotReemit_whenForegroundFindsNoChange() = runTest {
        val engine = FakePermissionEngine().apply { seed(Permission.Camera, PermissionStatus.Granted) }
        val manager = DefaultPermissionManager(engine, PermissionRegistry(), this)
        val emissions = mutableListOf<PermissionStatus>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) { manager.observe(Permission.Camera).toList(emissions) }

        engine.bringToForeground()

        assertEquals(listOf<PermissionStatus>(PermissionStatus.Granted), emissions)
        collector.cancel()
    }
}
