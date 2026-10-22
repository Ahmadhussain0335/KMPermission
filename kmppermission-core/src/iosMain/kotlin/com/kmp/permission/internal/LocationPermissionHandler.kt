package com.kmp.permission.internal

import com.kmp.permission.PermissionStatus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.darwin.NSObject
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private enum class LocationSignal { AuthorizationChanged, PromptShown, PromptDismissed }

private val PROMPT_APPEAR_TIMEOUT = 1.seconds
private val AUTHORIZATION_SETTLE_TIMEOUT = 500.milliseconds

private suspend fun Channel<LocationSignal>.awaitSignal(signal: LocationSignal) {
    while (receive() != signal) continue
}

/**
 * A single shared `CLLocationManager` + delegate, reused by all three location [IosPermissionHandler]s
 * below — `CLLocationManagerDelegate` only has one slot, so there must be exactly one manager per app.
 *
 * A `class` instantiated lazily rather than a top-level `object`: an eagerly-initialized singleton
 * extending `NSObject` triggers a Kotlin/Native compiler backend crash
 * (`NativeCodeGeneratorException` during global init codegen) as of Kotlin 2.4.0.
 *
 * The request functions must run on the main thread, which [PermissionController] guarantees along
 * with serializing them: the delegate-bearing manager has to be created on a thread with a run loop.
 */
private class LocationCoreImpl : NSObject(), CLLocationManagerDelegateProtocol {

    private var manager: CLLocationManager? = null
    private var signals: Channel<LocationSignal>? = null
    private var awaitingChangeFrom: CLAuthorizationStatus? = null

    // Reading status needs no delegate, so this one may be created on any thread.
    private val statusReader by lazy { CLLocationManager() }

    fun authorizationStatus(): CLAuthorizationStatus = statusReader.authorizationStatus

    private fun delegateManager(): CLLocationManager =
        manager ?: CLLocationManager().also {
            it.delegate = this
            manager = it
        }

    suspend fun requestWhenInUse() {
        if (authorizationStatus() != kCLAuthorizationStatusNotDetermined) return
        // From NotDetermined the prompt always appears and any answer changes the status.
        withSignals { channel ->
            delegateManager().requestWhenInUseAuthorization()
            channel.awaitSignal(LocationSignal.AuthorizationChanged)
        }
    }

    suspend fun requestAlways() {
        // "Always" can only be requested as an upgrade from "When In Use"; from any other state iOS
        // shows nothing and never calls the delegate.
        if (authorizationStatus() != kCLAuthorizationStatusAuthorizedWhenInUse) return

        // The upgrade prompt is shown at most once per install, and keeping "While Using" leaves the
        // status unchanged, so the delegate may never fire. App activity changes tell us whether a
        // prompt appeared and when it was dismissed.
        withSignals { channel ->
            val removeObservers = observeAppActivity { event ->
                channel.trySend(
                    when (event) {
                        AppActivityEvent.WillResignActive -> LocationSignal.PromptShown
                        AppActivityEvent.DidBecomeActive -> LocationSignal.PromptDismissed
                    },
                )
            }
            try {
                delegateManager().requestAlwaysAuthorization()
                val first = withTimeoutOrNull(PROMPT_APPEAR_TIMEOUT) { channel.receive() } ?: return@withSignals
                if (first == LocationSignal.AuthorizationChanged) return@withSignals
                while (true) {
                    when (channel.receive()) {
                        LocationSignal.AuthorizationChanged -> return@withSignals
                        LocationSignal.PromptDismissed -> {
                            // The delegate callback can trail reactivation slightly.
                            withTimeoutOrNull(AUTHORIZATION_SETTLE_TIMEOUT) {
                                channel.awaitSignal(LocationSignal.AuthorizationChanged)
                            }
                            return@withSignals
                        }
                        LocationSignal.PromptShown -> Unit
                    }
                }
            } finally {
                removeObservers()
            }
        }
    }

    private suspend fun withSignals(block: suspend (Channel<LocationSignal>) -> Unit) {
        val channel = Channel<LocationSignal>(Channel.UNLIMITED)
        awaitingChangeFrom = authorizationStatus()
        signals = channel
        try {
            block(channel)
        } finally {
            signals = null
            awaitingChangeFrom = null
            channel.close()
        }
    }

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        // iOS 14+ also calls this once when the delegate is first set, with the unchanged status.
        if (manager.authorizationStatus == awaitingChangeFrom) return
        signals?.trySend(LocationSignal.AuthorizationChanged)
    }
}

private val locationCore: LocationCoreImpl by lazy { LocationCoreImpl() }

private fun mapAuthorization(status: CLAuthorizationStatus): PermissionStatus = when (status) {
    kCLAuthorizationStatusNotDetermined -> PermissionStatus.NotDetermined
    kCLAuthorizationStatusRestricted -> PermissionStatus.Restricted
    kCLAuthorizationStatusDenied -> PermissionStatus.PermanentlyDenied
    kCLAuthorizationStatusAuthorizedAlways, kCLAuthorizationStatusAuthorizedWhenInUse -> PermissionStatus.Granted
    else -> PermissionStatus.Denied
}

internal class LocationFineHandler : IosPermissionHandler {

    // NOTE: iOS 14+ also has a separate "precise location" (CLAccuracyAuthorization) toggle
    // orthogonal to authorization status — a user can be authorized but have precise location
    // switched off in Settings. Distinguishing that here is a known future enhancement; for now
    // LocationFine and LocationCoarse both resolve from authorization status alone.
    override fun status(): PermissionStatus = mapAuthorization(locationCore.authorizationStatus())

    override suspend fun request(): PermissionStatus {
        locationCore.requestWhenInUse()
        return status()
    }
}

internal class LocationCoarseHandler : IosPermissionHandler {

    override fun status(): PermissionStatus = mapAuthorization(locationCore.authorizationStatus())

    override suspend fun request(): PermissionStatus {
        locationCore.requestWhenInUse()
        return status()
    }
}

internal class LocationBackgroundHandler : IosPermissionHandler {

    override fun status(): PermissionStatus =
        if (locationCore.authorizationStatus() == kCLAuthorizationStatusAuthorizedAlways) {
            PermissionStatus.Granted
        } else {
            mapAuthorization(locationCore.authorizationStatus()).let {
                if (it == PermissionStatus.Granted) PermissionStatus.Denied else it
            }
        }

    override suspend fun request(): PermissionStatus {
        // Apple requires "when in use" before an app can be upgraded to "always".
        locationCore.requestWhenInUse()
        locationCore.requestAlways()
        return status()
    }
}
