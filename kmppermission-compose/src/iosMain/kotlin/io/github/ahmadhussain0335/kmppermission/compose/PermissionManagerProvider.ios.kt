package io.github.ahmadhussain0335.kmppermission.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import io.github.ahmadhussain0335.kmppermission.PermissionManager

@Composable
internal actual fun rememberPermissionManager(): PermissionManager {
    val scope = rememberCoroutineScope()
    return remember(scope) { PermissionManager(scope) }
}
