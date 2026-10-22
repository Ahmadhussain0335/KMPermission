package com.kmp.permission.internal

import com.kmp.permission.PermissionStatus
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cValue
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.EventKit.EKAuthorizationStatus
import platform.EventKit.EKAuthorizationStatusDenied
import platform.EventKit.EKAuthorizationStatusNotDetermined
import platform.EventKit.EKAuthorizationStatusRestricted
import platform.EventKit.EKEntityType
import platform.EventKit.EKEventStore
import platform.Foundation.NSOperatingSystemVersion
import platform.Foundation.NSProcessInfo
import kotlin.coroutines.resume

internal class CalendarPermissionHandler : IosPermissionHandler {

    private val store = EKEventStore()

    override fun status(): PermissionStatus = map(EKEventStore.authorizationStatusForEntityType(EKEntityType.EKEntityTypeEvent))

    override suspend fun request(): PermissionStatus {
        if (EKEventStore.authorizationStatusForEntityType(EKEntityType.EKEntityTypeEvent) != EKAuthorizationStatusNotDetermined) {
            return status()
        }
        suspendCancellableCoroutine<Unit> { continuation ->
            if (isAtLeastIOS17()) {
                store.requestFullAccessToEventsWithCompletion { _, _ -> continuation.resume(Unit) }
            } else {
                @Suppress("DEPRECATION")
                store.requestAccessToEntityType(EKEntityType.EKEntityTypeEvent) { _, _ -> continuation.resume(Unit) }
            }
        }
        return status()
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun isAtLeastIOS17(): Boolean =
        NSProcessInfo.processInfo.isOperatingSystemAtLeastVersion(
            cValue<NSOperatingSystemVersion> {
                majorVersion = 17
                minorVersion = 0
                patchVersion = 0
            },
        )

    private fun map(status: EKAuthorizationStatus): PermissionStatus = when (status) {
        EKAuthorizationStatusNotDetermined -> PermissionStatus.NotDetermined
        EKAuthorizationStatusRestricted -> PermissionStatus.Restricted
        EKAuthorizationStatusDenied -> PermissionStatus.PermanentlyDenied
        // fullAccess and the iOS 17+ writeOnly grant are both treated as usable access.
        else -> PermissionStatus.Granted
    }
}
