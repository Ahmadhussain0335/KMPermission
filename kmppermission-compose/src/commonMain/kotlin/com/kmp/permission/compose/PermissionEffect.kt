package com.kmp.permission.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.kmp.permission.Permission
import com.kmp.permission.PermissionResult

/**
 * Requests [permissions] once, automatically, the first time this composable enters composition (or
 * whenever the permission set changes) — the Compose-idiomatic equivalent of [LaunchedEffect] for
 * permission requests, for screens that need a permission immediately rather than behind a button.
 */
@Composable
public fun PermissionEffect(
    vararg permissions: Permission,
    onResult: (Map<Permission, PermissionResult>) -> Unit,
) {
    val manager = rememberPermissionManager()
    val permissionsList = permissions.toList()
    val currentOnResult by rememberUpdatedState(onResult)

    LaunchedEffect(permissionsList) {
        currentOnResult(manager.request(*permissionsList.toTypedArray()))
    }
}
