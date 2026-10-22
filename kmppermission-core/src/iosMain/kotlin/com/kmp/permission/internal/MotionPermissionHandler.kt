package com.kmp.permission.internal

import com.kmp.permission.PermissionStatus
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreMotion.CMAuthorizationStatus
import platform.CoreMotion.CMAuthorizationStatusAuthorized
import platform.CoreMotion.CMAuthorizationStatusDenied
import platform.CoreMotion.CMAuthorizationStatusNotDetermined
import platform.CoreMotion.CMAuthorizationStatusRestricted
import platform.CoreMotion.CMMotionActivityManager
import platform.Foundation.NSDate
import platform.Foundation.NSOperationQueue
import platform.Foundation.dateByAddingTimeInterval
import kotlin.coroutines.resume

internal class MotionPermissionHandler : IosPermissionHandler {

    override fun status(): PermissionStatus = map(CMMotionActivityManager.authorizationStatus())

    override suspend fun request(): PermissionStatus {
        if (CMMotionActivityManager.authorizationStatus() != CMAuthorizationStatusNotDetermined) {
            return status()
        }
        // CoreMotion has no explicit "request authorization" call either — the system prompt appears
        // the first time historical activity data is queried.
        val manager = CMMotionActivityManager()
        val now = NSDate()
        val oneMinuteAgo = now.dateByAddingTimeInterval(-60.0)
        suspendCancellableCoroutine<Unit> { continuation ->
            manager.queryActivityStartingFromDate(oneMinuteAgo, toDate = now, toQueue = NSOperationQueue.mainQueue) { _, _ ->
                continuation.resume(Unit)
            }
        }
        return status()
    }

    private fun map(status: CMAuthorizationStatus): PermissionStatus = when (status) {
        CMAuthorizationStatusNotDetermined -> PermissionStatus.NotDetermined
        CMAuthorizationStatusRestricted -> PermissionStatus.Restricted
        CMAuthorizationStatusDenied -> PermissionStatus.PermanentlyDenied
        CMAuthorizationStatusAuthorized -> PermissionStatus.Granted
        else -> PermissionStatus.Denied
    }
}
