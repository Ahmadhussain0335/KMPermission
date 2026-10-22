package com.kmp.permission.internal

import com.kmp.permission.PermissionStatus
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Photos.PHAccessLevelReadWrite
import platform.Photos.PHAuthorizationStatus
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHAuthorizationStatusDenied
import platform.Photos.PHAuthorizationStatusLimited
import platform.Photos.PHAuthorizationStatusNotDetermined
import platform.Photos.PHAuthorizationStatusRestricted
import platform.Photos.PHPhotoLibrary
import kotlin.coroutines.resume

internal class PhotosPermissionHandler : IosPermissionHandler {

    override fun status(): PermissionStatus =
        map(PHPhotoLibrary.authorizationStatusForAccessLevel(PHAccessLevelReadWrite))

    override suspend fun request(): PermissionStatus {
        if (PHPhotoLibrary.authorizationStatusForAccessLevel(PHAccessLevelReadWrite) != PHAuthorizationStatusNotDetermined) {
            return status()
        }
        suspendCancellableCoroutine<Unit> { continuation ->
            PHPhotoLibrary.requestAuthorizationForAccessLevel(PHAccessLevelReadWrite) {
                continuation.resume(Unit)
            }
        }
        return status()
    }

    private fun map(status: PHAuthorizationStatus): PermissionStatus = when (status) {
        PHAuthorizationStatusNotDetermined -> PermissionStatus.NotDetermined
        PHAuthorizationStatusRestricted -> PermissionStatus.Restricted
        PHAuthorizationStatusDenied -> PermissionStatus.PermanentlyDenied
        // Limited: the user picked specific photos rather than granting full access — still usable,
        // so treated as Granted rather than introducing a sixth PermissionStatus case for it.
        PHAuthorizationStatusAuthorized, PHAuthorizationStatusLimited -> PermissionStatus.Granted
        else -> PermissionStatus.Denied
    }
}
