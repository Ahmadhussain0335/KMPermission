package com.kmp.permission.internal

import com.kmp.permission.PermissionStatus
import kotlinx.coroutines.runBlocking
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
import platform.UserNotifications.UNNotificationSettings
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume

internal class NotificationsPermissionHandler : IosPermissionHandler {

    // UNUserNotificationCenter only exposes settings through an async completion handler, even
    // though reading them performs no real I/O. Blocking here keeps `status()` synchronous, matching
    // every other permission in this library — the wait is effectively instant in practice.
    private fun currentSettings(): UNNotificationSettings = runBlocking {
        suspendCancellableCoroutine { continuation ->
            UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { settings ->
                continuation.resume(settings!!)
            }
        }
    }

    override fun status(): PermissionStatus = map(currentSettings().authorizationStatus)

    override suspend fun request(): PermissionStatus {
        if (currentSettings().authorizationStatus != UNAuthorizationStatusNotDetermined) return status()
        val options = UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound
        suspendCancellableCoroutine<Unit> { continuation ->
            UNUserNotificationCenter.currentNotificationCenter()
                .requestAuthorizationWithOptions(options) { _, _ -> continuation.resume(Unit) }
        }
        return status()
    }

    private fun map(status: UNAuthorizationStatus): PermissionStatus = when (status) {
        UNAuthorizationStatusNotDetermined -> PermissionStatus.NotDetermined
        UNAuthorizationStatusDenied -> PermissionStatus.PermanentlyDenied
        UNAuthorizationStatusAuthorized, UNAuthorizationStatusProvisional, UNAuthorizationStatusEphemeral -> PermissionStatus.Granted
        else -> PermissionStatus.Denied
    }
}
