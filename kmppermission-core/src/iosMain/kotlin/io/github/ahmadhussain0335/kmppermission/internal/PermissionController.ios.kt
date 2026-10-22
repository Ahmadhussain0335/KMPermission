package io.github.ahmadhussain0335.kmppermission.internal

import io.github.ahmadhussain0335.kmppermission.Permission
import io.github.ahmadhussain0335.kmppermission.PermissionException
import io.github.ahmadhussain0335.kmppermission.PermissionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import platform.AVFoundation.AVMediaTypeAudio
import platform.AVFoundation.AVMediaTypeVideo
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * Process-wide because the Bluetooth and Location handlers share one native manager each, and iOS
 * shows one system prompt at a time anyway.
 */
private val requestMutex = Mutex()

/**
 * Real iOS runtime engine. Dispatches each [Permission] to the native Apple framework handler that
 * owns it — see [IosPermissionHandler] for the shared status/request contract every handler follows.
 */
internal actual class PermissionController actual constructor() : PermissionEngine {

    private fun handlerFor(permission: Permission): IosPermissionHandler = when (permission) {
        Permission.Camera -> AvMediaPermissionHandler(AVMediaTypeVideo)
        Permission.Microphone -> AvMediaPermissionHandler(AVMediaTypeAudio)
        Permission.Photos -> PhotosPermissionHandler()
        Permission.Bluetooth, Permission.BluetoothScan, Permission.BluetoothConnect, Permission.BluetoothAdvertise ->
            BluetoothPermissionHandler
        Permission.LocationFine -> LocationFineHandler()
        Permission.LocationCoarse -> LocationCoarseHandler()
        Permission.LocationBackground -> LocationBackgroundHandler()
        Permission.Notifications -> NotificationsPermissionHandler()
        Permission.Contacts -> ContactsPermissionHandler()
        Permission.Calendar -> CalendarPermissionHandler()
        Permission.Motion -> MotionPermissionHandler()
        Permission.SpeechRecognition -> SpeechRecognitionPermissionHandler()
        Permission.Storage, Permission.Nfc, Permission.NearbyWifiDevices, Permission.LocalNetwork -> throw PermissionException.UnsupportedPermission(permission, "iOS")
    }

    actual override fun status(permission: Permission): PermissionStatus {
        InfoPlistChecker.requireDeclared(permission)
        return handlerFor(permission).status()
    }

    actual override suspend fun request(permissions: Set<Permission>): Map<Permission, PermissionStatus> {
        permissions.forEach(InfoPlistChecker::requireDeclared)
        // Native managers deliver their callbacks on the thread they were created on, and that
        // thread needs a run loop, so every request runs on main.
        return withContext(Dispatchers.Main) {
            requestMutex.withLock { permissions.associateWith { handlerFor(it).request() } }
        }
    }

    actual override val foregroundEvents: Flow<Unit> = callbackFlow {
        val removeObservers = observeAppActivity { event ->
            if (event == AppActivityEvent.DidBecomeActive) trySend(Unit)
        }
        awaitClose { removeObservers() }
    }

    actual override fun openSettings() {
        val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString) ?: return
        dispatch_async(dispatch_get_main_queue()) {
            UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any>()) {}
        }
    }
}
