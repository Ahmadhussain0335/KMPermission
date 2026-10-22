package io.github.ahmadhussain0335.kmppermission.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import io.github.ahmadhussain0335.kmppermission.Permission
import io.github.ahmadhussain0335.kmppermission.PermissionResult
import kotlinx.coroutines.launch

/**
 * An imperative trigger for a permission request, obtained via [rememberPermissionLauncher] —
 * mirrors the shape of Compose's own `rememberLauncherForActivityResult` for call sites (e.g. a
 * `Button`'s `onClick`) that need to fire a request without a full [PermissionState].
 */
@Stable
public fun interface PermissionLauncher {
    public fun launch(vararg permissions: Permission)
}

@Composable
public fun rememberPermissionLauncher(
    onResult: (Map<Permission, PermissionResult>) -> Unit,
): PermissionLauncher {
    val manager = rememberPermissionManager()
    val scope = rememberCoroutineScope()
    val currentOnResult by rememberUpdatedState(onResult)
    return remember(manager) {
        PermissionLauncher { permissions ->
            scope.launch { currentOnResult(manager.request(*permissions)) }
        }
    }
}
