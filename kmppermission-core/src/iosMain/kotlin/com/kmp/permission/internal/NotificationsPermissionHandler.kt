package com.kmp.permission.internal

import com.kmp.permission.PermissionStatus
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatus
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNAuthorizationStatusDenied
import platform.UserNotifications.UNAuthorizationStatusEphemeral
import platform.UserNotifications.UNAuthorizationStatusNotDetermined
import platform.UserNotifications.UNAuthorizationStatusProvisional
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.dispatch_semaphore_create
import platform.darwin.dispatch_semaphore_signal
import platform.darwin.dispatch_semaphore_wait
import platform.darwin.dispatch_time
import kotlin.coroutines.resume

private const val STATUS_TIMEOUT_NANOS = 500_000_000L

/** Last status read from the system; returned if a synchronous read times out. */
private var lastKnownStatus: UNAuthorizationStatus = UNAuthorizationStatusNotDetermined

internal class NotificationsPermissionHandler : IosPermissionHandler {

    private val center get() = UNUserNotificationCenter.currentNotificationCenter()

    override fun status(): PermissionStatus = map(readStatusBlocking())

    override suspend fun request(): PermissionStatus {
        val current = readStatus()
        if (current != UNAuthorizationStatusNotDetermined) return map(current)
        val options = UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound
        suspendCancellableCoroutine<Unit> { continuation ->
            center.requestAuthorizationWithOptions(options) { _, _ -> continuation.resume(Unit) }
        }
        return map(readStatus())
    }

    private suspend fun readStatus(): UNAuthorizationStatus = suspendCancellableCoroutine { continuation ->
        center.getNotificationSettingsWithCompletionHandler { settings ->
            val status = settings?.authorizationStatus ?: UNAuthorizationStatusNotDetermined
            lastKnownStatus = status
            continuation.resume(status)
        }
    }

    // status() is synchronous by contract, but notification settings are only available through a
    // completion handler. That handler runs on a background queue, so waiting cannot deadlock the
    // calling thread; the wait is still bounded so a slow system never stalls the UI.
    private fun readStatusBlocking(): UNAuthorizationStatus {
        val semaphore = dispatch_semaphore_create(0)
        center.getNotificationSettingsWithCompletionHandler { settings ->
            settings?.authorizationStatus?.let { lastKnownStatus = it }
            dispatch_semaphore_signal(semaphore)
        }
        dispatch_semaphore_wait(semaphore, dispatch_time(DISPATCH_TIME_NOW, STATUS_TIMEOUT_NANOS))
        return lastKnownStatus
    }

    private fun map(status: UNAuthorizationStatus): PermissionStatus = when (status) {
        UNAuthorizationStatusNotDetermined -> PermissionStatus.NotDetermined
        UNAuthorizationStatusDenied -> PermissionStatus.PermanentlyDenied
        UNAuthorizationStatusAuthorized, UNAuthorizationStatusProvisional, UNAuthorizationStatusEphemeral -> PermissionStatus.Granted
        else -> PermissionStatus.Denied
    }
}
