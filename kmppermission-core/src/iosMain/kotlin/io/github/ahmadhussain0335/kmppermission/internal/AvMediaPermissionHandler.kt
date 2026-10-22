package io.github.ahmadhussain0335.kmppermission.internal

import io.github.ahmadhussain0335.kmppermission.PermissionStatus
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.AVFoundation.AVAuthorizationStatus
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaType
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import kotlin.coroutines.resume

/** Camera and microphone both live under `AVCaptureDevice`, differing only by [mediaType]. */
internal class AvMediaPermissionHandler(private val mediaType: AVMediaType) : IosPermissionHandler {

    override fun status(): PermissionStatus = map(AVCaptureDevice.authorizationStatusForMediaType(mediaType))

    override suspend fun request(): PermissionStatus {
        if (AVCaptureDevice.authorizationStatusForMediaType(mediaType) != AVAuthorizationStatusNotDetermined) {
            return status()
        }
        val granted = suspendCancellableCoroutine { continuation ->
            AVCaptureDevice.requestAccessForMediaType(mediaType) { continuation.resume(it) }
        }
        return status().orIfUndetermined(grantedToStatus(granted))
    }

    private fun map(status: AVAuthorizationStatus): PermissionStatus = when (status) {
        AVAuthorizationStatusNotDetermined -> PermissionStatus.NotDetermined
        AVAuthorizationStatusRestricted -> PermissionStatus.Restricted
        AVAuthorizationStatusDenied -> PermissionStatus.PermanentlyDenied
        AVAuthorizationStatusAuthorized -> PermissionStatus.Granted
        else -> PermissionStatus.Denied
    }
}
