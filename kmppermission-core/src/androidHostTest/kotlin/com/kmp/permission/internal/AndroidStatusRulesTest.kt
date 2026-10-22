package com.kmp.permission.internal

import com.kmp.permission.Permission
import com.kmp.permission.PermissionStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidStatusRulesTest {

    /** Runs one completed, denied request through the rules and returns the resulting status and history. */
    private fun deny(
        history: RequestHistory,
        canShowRationale: Boolean,
        answeredInstantly: Boolean = false,
    ): Pair<PermissionStatus, RequestHistory> {
        val updated = AndroidStatusRules.afterRequest(
            granted = false,
            canShowRationale = canShowRationale,
            history = history,
            answeredInstantly = answeredInstantly,
        )
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
            AndroidStatusRules.status(granted = true, canShowRationale = false, history = RequestHistory(true, true, true)),
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
    fun repeatedlyDismissedDialog_staysDenied() {
        val (_, afterFirst) = deny(RequestHistory(), canShowRationale = false)
        val (status, _) = deny(afterFirst, canShowRationale = false)

        assertEquals(PermissionStatus.Denied, status)
    }

    @Test
    fun instantDenialWithoutRationale_isPermanentlyDenied() {
        val (status, _) = deny(RequestHistory(), canShowRationale = false, answeredInstantly = true)

        assertEquals(PermissionStatus.PermanentlyDenied, status)
    }

    @Test
    fun instantDenialWithRationale_isDenied() {
        val (status, _) = deny(RequestHistory(), canShowRationale = true, answeredInstantly = true)

        assertEquals(PermissionStatus.Denied, status)
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
        val history = AndroidStatusRules.observed(granted = false, canShowRationale = true, history = RequestHistory(requested = true))

        assertEquals(RequestHistory(requested = true, rationaleSeen = true), history)
    }

    @Test
    fun backgroundLocationRationale_isIgnored_untilRequested() {
        assertFalse(AndroidStatusRules.effectiveRationale(Permission.LocationBackground, rawRationale = true, history = RequestHistory()))
        assertTrue(AndroidStatusRules.effectiveRationale(Permission.LocationBackground, rawRationale = true, history = RequestHistory(requested = true)))
    }

    @Test
    fun otherPermissionsRationale_isUsedAsIs() {
        assertTrue(AndroidStatusRules.effectiveRationale(Permission.Camera, rawRationale = true, history = RequestHistory()))
    }
}
