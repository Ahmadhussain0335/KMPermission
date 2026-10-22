package io.github.ahmadhussain0335.kmppermission.internal

import io.github.ahmadhussain0335.kmppermission.PermissionStatus
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
        val answered = suspendCancellableCoroutine { continuation ->
            PHPhotoLibrary.requestAuthorizationForAccessLevel(PHAccessLevelReadWrite) { continuation.resume(it) }
        }
        return status().orIfUndetermined(map(answered))
    }

    private fun map(status: PHAuthorizationStatus): PermissionStatus = when (status) {
        PHAuthorizationStatusNotDetermined -> PermissionStatus.NotDetermined
        PHAuthorizationStatusRestricted -> PermissionStatus.Restricted
        PHAuthorizationStatusDenied -> PermissionStatus.PermanentlyDenied
        PHAuthorizationStatusAuthorized -> PermissionStatus.Granted
        PHAuthorizationStatusLimited -> PermissionStatus.Limited
        else -> PermissionStatus.Denied
    }
}
