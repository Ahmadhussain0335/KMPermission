package io.github.ahmadhussain0335.kmppermission.internal

import io.github.ahmadhussain0335.kmppermission.PermissionStatus
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBCentralManagerDelegateProtocol
import platform.CoreBluetooth.CBCentralManagerOptionShowPowerAlertKey
import platform.CoreBluetooth.CBManager
import platform.CoreBluetooth.CBManagerAuthorization
import platform.CoreBluetooth.CBManagerAuthorizationAllowedAlways
import platform.CoreBluetooth.CBManagerAuthorizationDenied
import platform.CoreBluetooth.CBManagerAuthorizationNotDetermined
import platform.CoreBluetooth.CBManagerAuthorizationRestricted
import platform.darwin.NSObject
import kotlin.coroutines.resume

/**
 * Core Bluetooth has no explicit "request authorization" call — the system prompt appears the first
 * time a `CBCentralManager` is instantiated.
 *
 * Kept separate from [BluetoothPermissionHandler] because Kotlin/Native does not allow a single class
 * to extend an Objective-C type (`NSObject`/`CBCentralManagerDelegateProtocol`) and implement a plain
 * Kotlin interface ([IosPermissionHandler]) at the same time.
 *
 * A `class` instantiated lazily rather than a top-level `object`: an eagerly-initialized singleton
 * extending `NSObject` triggers a Kotlin/Native compiler backend crash
 * (`NativeCodeGeneratorException` during global init codegen) as of Kotlin 2.4.0.
 */
private class BluetoothCoreImpl : NSObject(), CBCentralManagerDelegateProtocol {

    private var manager: CBCentralManager? = null
    private var pendingContinuation: CancellableContinuation<Unit>? = null

    fun authorization(): CBManagerAuthorization = CBManager.authorization

    /**
     * Must run on the main thread: `queue = null` delivers delegate callbacks on the main queue.
     * [PermissionController] guarantees that and serializes calls, so one pending continuation is enough.
     */
    suspend fun request() {
        if (authorization() != CBManagerAuthorizationNotDetermined) return
        // Backstop in case no further state update arrives once the user has answered.
        val removeObservers = observeAppActivity { if (it == AppActivityEvent.DidBecomeActive) resumeIfDecided() }
        try {
            awaitDecision()
        } finally {
            removeObservers()
        }
    }

    private suspend fun awaitDecision() {
        suspendCancellableCoroutine { continuation ->
            pendingContinuation = continuation
            continuation.invokeOnCancellation { pendingContinuation = null }
            manager = CBCentralManager(
                delegate = this,
                queue = null,
                // Only the authorization prompt is wanted here, not the "Turn On Bluetooth" alert.
                options = mapOf<Any?, Any>(CBCentralManagerOptionShowPowerAlertKey to false),
            )
        }
    }

    override fun centralManagerDidUpdateState(central: CBCentralManager) = resumeIfDecided()

    private fun resumeIfDecided() {
        // State updates can arrive before the user has answered the prompt.
        if (authorization() == CBManagerAuthorizationNotDetermined) return
        val continuation = pendingContinuation ?: return
        pendingContinuation = null
        continuation.resume(Unit)
    }
}

private val bluetoothCore: BluetoothCoreImpl by lazy { BluetoothCoreImpl() }

/**
 * [Permission.Bluetooth], [Permission.BluetoothScan], [Permission.BluetoothConnect], and
 * [Permission.BluetoothAdvertise] all share
 * this single authorization on iOS, so one handler covers all three (see [PermissionController]'s
 * dispatch).
 */
internal object BluetoothPermissionHandler : IosPermissionHandler {

    override fun status(): PermissionStatus = map(bluetoothCore.authorization())

    override suspend fun request(): PermissionStatus {
        bluetoothCore.request()
        return status()
    }

    private fun map(authorization: CBManagerAuthorization): PermissionStatus = when (authorization) {
        CBManagerAuthorizationNotDetermined -> PermissionStatus.NotDetermined
        CBManagerAuthorizationRestricted -> PermissionStatus.Restricted
        CBManagerAuthorizationDenied -> PermissionStatus.PermanentlyDenied
        CBManagerAuthorizationAllowedAlways -> PermissionStatus.Granted
        else -> PermissionStatus.Denied
    }
}
