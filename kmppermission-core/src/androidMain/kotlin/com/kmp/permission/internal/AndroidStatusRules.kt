package com.kmp.permission.internal

import com.kmp.permission.PermissionStatus

/**
 * Pure decision logic behind [PermissionStatusResolver], kept free of Android types so it can be
 * unit tested on the host.
 */
internal object AndroidStatusRules {

    fun status(granted: Boolean, canShowRationale: Boolean, history: RequestHistory): PermissionStatus = when {
        granted -> PermissionStatus.Granted
        canShowRationale -> PermissionStatus.Denied
        // Android only stops prompting after a denial that followed a rationale-eligible one, or
        // after two denials on Android 11+.
        history.rationaleSeen || history.deniedCount >= 2 -> PermissionStatus.PermanentlyDenied
        history.deniedCount == 0 -> PermissionStatus.NotDetermined
        // A single denial with no rationale is a dismissed dialog: the prompt will show again.
        else -> PermissionStatus.Denied
    }

    /** History to store after observing the current state, outside of a request. */
    fun observed(granted: Boolean, canShowRationale: Boolean, history: RequestHistory): RequestHistory = when {
        // Resetting on a grant means a later revoke (from Settings, or auto-revoke of unused apps)
        // starts fresh, since the system prompts again in both cases.
        granted -> RequestHistory()
        canShowRationale -> history.copy(rationaleSeen = true)
        else -> history
    }

    /** History to store once a request for the permission has completed. */
    fun afterRequest(granted: Boolean, canShowRationale: Boolean, history: RequestHistory): RequestHistory =
        if (granted) {
            RequestHistory()
        } else {
            RequestHistory(
                deniedCount = history.deniedCount + 1,
                rationaleSeen = history.rationaleSeen || canShowRationale,
            )
        }
}
