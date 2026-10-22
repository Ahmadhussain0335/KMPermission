package io.github.ahmadhussain0335.kmppermission.internal

import io.github.ahmadhussain0335.kmppermission.PermissionStatus
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Speech.SFSpeechRecognizer
import platform.Speech.SFSpeechRecognizerAuthorizationStatus
import kotlin.coroutines.resume

internal class SpeechRecognitionPermissionHandler : IosPermissionHandler {

    override fun status(): PermissionStatus = map(SFSpeechRecognizer.authorizationStatus())

    override suspend fun request(): PermissionStatus {
        if (SFSpeechRecognizer.authorizationStatus() != SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusNotDetermined) {
            return status()
        }
        val answered = suspendCancellableCoroutine { continuation ->
            SFSpeechRecognizer.requestAuthorization { continuation.resume(it) }
        }
        return status().orIfUndetermined(map(answered))
    }

    private fun map(status: SFSpeechRecognizerAuthorizationStatus): PermissionStatus = when (status) {
        SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusNotDetermined -> PermissionStatus.NotDetermined
        SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusRestricted -> PermissionStatus.Restricted
        SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusDenied -> PermissionStatus.PermanentlyDenied
        SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized -> PermissionStatus.Granted
        else -> PermissionStatus.Denied
    }
}
