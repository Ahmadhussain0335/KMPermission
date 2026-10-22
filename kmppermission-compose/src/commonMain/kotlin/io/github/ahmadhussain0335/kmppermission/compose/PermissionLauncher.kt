package io.github.ahmadhussain0335.kmppermission.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import io.github.ahmadhussain0335.kmppermission.Permission
import io.github.ahmadhussain0335.kmppermission.PermissionException
import io.github.ahmadhussain0335.kmppermission.PermissionManager
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

/**
 * Remembers a [PermissionLauncher] that reports each request's results to [onResult].
 *
 * @param onError receives configuration errors (a missing manifest or Info.plist entry, or a
 *   permission unsupported on this platform) instead of [onResult]. The default rethrows them,
 *   which fails fast; pass a handler to show or log them without crashing.
 */
@Composable
public fun rememberPermissionLauncher(
    onError: (PermissionException) -> Unit = { throw it },
    onResult: (Map<Permission, PermissionResult>) -> Unit,
): PermissionLauncher = rememberPermissionLauncher(rememberPermissionManager(), onError, onResult)

@Composable
internal fun rememberPermissionLauncher(
    manager: PermissionManager,
    onError: (PermissionException) -> Unit,
    onResult: (Map<Permission, PermissionResult>) -> Unit,
): PermissionLauncher {
    val scope = rememberCoroutineScope()
    val currentOnError by rememberUpdatedState(onError)
    val currentOnResult by rememberUpdatedState(onResult)
    return remember(manager) {
        PermissionLauncher { permissions ->
            scope.launch {
                val results = try {
                    manager.request(*permissions)
                } catch (e: PermissionException) {
                    currentOnError(e)
                    return@launch
                }
                currentOnResult(results)
            }
        }
    }
}
