package com.kmp.permission.internal

import com.kmp.permission.PermissionStatus
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.darwin.NSObject
import kotlin.coroutines.resume

/**
 * A single shared `CLLocationManager` + delegate, reused by all three location [IosPermissionHandler]s
 * below — `CLLocationManagerDelegate` only has one slot, so there must be exactly one manager per app.
 *
 * A `class` instantiated lazily rather than a top-level `object`: an eagerly-initialized singleton
 * extending `NSObject` triggers a Kotlin/Native compiler backend crash
 * (`NativeCodeGeneratorException` during global init codegen) as of Kotlin 2.4.0.
 */
private class LocationCoreImpl : NSObject(), CLLocationManagerDelegateProtocol {

    private val manager = CLLocationManager().apply { delegate = this@LocationCoreImpl }
    private var pendingContinuation: CancellableContinuation<Unit>? = null

    fun authorizationStatus(): CLAuthorizationStatus = manager.authorizationStatus

    suspend fun requestWhenInUse() {
        if (authorizationStatus() != kCLAuthorizationStatusNotDetermined) return
        suspendCancellableCoroutine { continuation ->
            pendingContinuation = continuation
            manager.requestWhenInUseAuthorization()
        }
    }

    suspend fun requestAlways() {
        if (authorizationStatus() == kCLAuthorizationStatusAuthorizedAlways) return
        suspendCancellableCoroutine { continuation ->
            pendingContinuation = continuation
            manager.requestAlwaysAuthorization()
        }
    }

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        pendingContinuation?.resume(Unit)
        pendingContinuation = null
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
