package com.kmp.permission.internal

import com.kmp.permission.PermissionStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class AndroidStatusRulesTest {

    /** Runs one completed request through the rules and returns the resulting status and history. */
    private fun deny(history: RequestHistory, canShowRationale: Boolean): Pair<PermissionStatus, RequestHistory> {
        val updated = AndroidStatusRules.afterRequest(granted = false, canShowRationale = canShowRationale, history = history)
        return AndroidStatusRules.status(granted = false, canShowRationale = canShowRationale, history = updated) to updated
    }

    @Test
    fun status_isNotDetermined_whenNeverRequested() {
        assertEquals(
            PermissionStatus.NotDetermined,
            AndroidStatusRules.status(granted = false, canShowRationale = false, history = RequestHistory()),
        )
    }

    @Test
    fun status_isGranted_whenGranted_regardlessOfHistory() {
        assertEquals(
            PermissionStatus.Granted,
            AndroidStatusRules.status(granted = true, canShowRationale = false, history = RequestHistory(5, true)),
        )
    }

    @Test
    fun firstDenial_withRationale_isDenied() {
        val (status, _) = deny(RequestHistory(), canShowRationale = true)

        assertEquals(PermissionStatus.Denied, status)
    }

    @Test
    fun denialAfterRationale_withoutRationale_isPermanentlyDenied() {
        val (_, afterFirst) = deny(RequestHistory(), canShowRationale = true)
        val (status, _) = deny(afterFirst, canShowRationale = false)

        assertEquals(PermissionStatus.PermanentlyDenied, status)
    }

    @Test
    fun singleDismissedDialog_isDenied_notPermanentlyDenied() {
        val (status, _) = deny(RequestHistory(), canShowRationale = false)

        assertEquals(PermissionStatus.Denied, status)
    }

    @Test
    fun twoDenialsWithoutRationale_arePermanentlyDenied() {
        val (_, afterFirst) = deny(RequestHistory(), canShowRationale = false)
        val (status, _) = deny(afterFirst, canShowRationale = false)

        assertEquals(PermissionStatus.PermanentlyDenied, status)
    }

    @Test
    fun observedGrant_resetsHistory_soLaterRevokeIsNotDetermined() {
        val (_, denied) = deny(RequestHistory(), canShowRationale = true)
        val afterGrant = AndroidStatusRules.observed(granted = true, canShowRationale = false, history = denied)

        assertEquals(RequestHistory(), afterGrant)
        assertEquals(
            PermissionStatus.NotDetermined,
            AndroidStatusRules.status(granted = false, canShowRationale = false, history = afterGrant),
        )
    }

    @Test
    fun observedRationale_isRemembered() {
        val history = AndroidStatusRules.observed(granted = false, canShowRationale = true, history = RequestHistory(1, false))

        assertEquals(RequestHistory(1, true), history)
    }
}
