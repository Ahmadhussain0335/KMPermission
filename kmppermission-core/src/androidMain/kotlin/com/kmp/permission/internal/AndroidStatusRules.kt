package com.kmp.permission.internal

import com.kmp.permission.PermissionStatus

/**
 * Pure decision logic behind [PermissionStatusResolver], kept free of Android types so it can be
 * unit tested on the host.
 */
internal object AndroidStatusRules {

    /**
     * A result faster than this cannot have come from a person answering a dialog, so the system
     * refused without showing one.
     */
    const val AUTO_DENY_THRESHOLD_MS = 500L

    fun status(granted: Boolean, canShowRationale: Boolean, history: RequestHistory): PermissionStatus = when {
        granted -> PermissionStatus.Granted
        canShowRationale -> PermissionStatus.Denied
        // Rationale stops being offered after the denial that follows a rationale-eligible one.
        history.rationaleSeen || history.autoDenied -> PermissionStatus.PermanentlyDenied
        !history.requested -> PermissionStatus.NotDetermined
        // Denied without ever offering a rationale: a dismissed dialog, which will show again.
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
    fun afterRequest(
        granted: Boolean,
        canShowRationale: Boolean,
        history: RequestHistory,
        answeredInstantly: Boolean,
    ): RequestHistory =
        if (granted) {
            RequestHistory()
        } else {
            RequestHistory(
                requested = true,
                rationaleSeen = history.rationaleSeen || canShowRationale,
                autoDenied = history.autoDenied || (answeredInstantly && !canShowRationale),
            )
        }
}
