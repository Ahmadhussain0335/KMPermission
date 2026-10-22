package com.kmp.permission.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.kmp.permission.PermissionManager

@Composable
internal actual fun rememberPermissionManager(): PermissionManager {
    val scope = rememberCoroutineScope()
    return remember(scope) { PermissionManager(scope) }
}
