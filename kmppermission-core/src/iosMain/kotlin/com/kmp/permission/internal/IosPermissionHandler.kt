package com.kmp.permission.internal

import com.kmp.permission.PermissionStatus

/**
 * One native Apple framework's worth of permission logic, dispatched to by [PermissionController].
 *
 * Every implementation maps its native authorization enum onto [PermissionStatus] the same way: a
 * denial on iOS is final — re-requesting after an explicit "Don't Allow" shows no system UI at all,
 * unlike Android's rationale-eligible first denial — so a native "denied" always maps to
 * [PermissionStatus.PermanentlyDenied], never [PermissionStatus.Denied]. iOS has no `Denied` case, in
 * other words: only [PermissionStatus.NotDetermined] is retriable via a system prompt.
 */
internal interface IosPermissionHandler {

    fun status(): PermissionStatus

    suspend fun request(): PermissionStatus
}
