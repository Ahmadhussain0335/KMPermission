package com.kmp.permission.internal

import com.kmp.permission.PermissionStatus
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.EventKit.EKAuthorizationStatus
import platform.EventKit.EKAuthorizationStatusDenied
import platform.EventKit.EKAuthorizationStatusFullAccess
import platform.EventKit.EKAuthorizationStatusNotDetermined
import platform.EventKit.EKAuthorizationStatusRestricted
import platform.EventKit.EKAuthorizationStatusWriteOnly
import platform.EventKit.EKEntityType
import platform.EventKit.EKEventStore
import kotlin.coroutines.resume

internal class CalendarPermissionHandler : IosPermissionHandler {

    private val store = EKEventStore()

    override fun status(): PermissionStatus = map(EKEventStore.authorizationStatusForEntityType(EKEntityType.EKEntityTypeEvent))

    override suspend fun request(): PermissionStatus {
        if (EKEventStore.authorizationStatusForEntityType(EKEntityType.EKEntityTypeEvent) != EKAuthorizationStatusNotDetermined) {
            return status()
        }
        suspendCancellableCoroutine<Unit> { continuation ->
            if (iosMajorVersion >= 17) {
                store.requestFullAccessToEventsWithCompletion { _, _ -> continuation.resume(Unit) }
            } else {
                @Suppress("DEPRECATION")
                store.requestAccessToEntityType(EKEntityType.EKEntityTypeEvent) { _, _ -> continuation.resume(Unit) }
            }
        }
        return status()
    }

    private fun map(status: EKAuthorizationStatus): PermissionStatus = when (status) {
        EKAuthorizationStatusNotDetermined -> PermissionStatus.NotDetermined
        EKAuthorizationStatusRestricted -> PermissionStatus.Restricted
        EKAuthorizationStatusDenied -> PermissionStatus.PermanentlyDenied
        // FullAccess has the same raw value as the pre-iOS 17 Authorized status.
        EKAuthorizationStatusFullAccess -> PermissionStatus.Granted
        EKAuthorizationStatusWriteOnly -> PermissionStatus.Limited
        else -> PermissionStatus.Denied
    }
}
