package com.kmp.permission.internal

import com.kmp.permission.PermissionStatus
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Contacts.CNAuthorizationStatus
import platform.Contacts.CNAuthorizationStatusAuthorized
import platform.Contacts.CNAuthorizationStatusDenied
import platform.Contacts.CNAuthorizationStatusLimited
import platform.Contacts.CNAuthorizationStatusNotDetermined
import platform.Contacts.CNAuthorizationStatusRestricted
import platform.Contacts.CNContactStore
import platform.Contacts.CNEntityType
import kotlin.coroutines.resume

internal class ContactsPermissionHandler : IosPermissionHandler {

    override fun status(): PermissionStatus = map(CNContactStore.authorizationStatusForEntityType(CNEntityType.CNEntityTypeContacts))

    override suspend fun request(): PermissionStatus {
        if (CNContactStore.authorizationStatusForEntityType(CNEntityType.CNEntityTypeContacts) != CNAuthorizationStatusNotDetermined) {
            return status()
        }
        val granted = suspendCancellableCoroutine { continuation ->
            CNContactStore().requestAccessForEntityType(CNEntityType.CNEntityTypeContacts) { granted, _ ->
                continuation.resume(granted)
            }
        }
        return status().orIfUndetermined(grantedToStatus(granted))
    }

    private fun map(status: CNAuthorizationStatus): PermissionStatus = when (status) {
        CNAuthorizationStatusNotDetermined -> PermissionStatus.NotDetermined
        CNAuthorizationStatusRestricted -> PermissionStatus.Restricted
        CNAuthorizationStatusDenied -> PermissionStatus.PermanentlyDenied
        CNAuthorizationStatusAuthorized -> PermissionStatus.Granted
        CNAuthorizationStatusLimited -> PermissionStatus.Limited
        else -> PermissionStatus.Denied
    }
}
