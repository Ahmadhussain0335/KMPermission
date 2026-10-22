package io.github.ahmadhussain0335.kmppermission.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import io.github.ahmadhussain0335.kmppermission.Permission
import io.github.ahmadhussain0335.kmppermission.PermissionManager
import io.github.ahmadhussain0335.kmppermission.PermissionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Observable Compose state for a single [Permission], obtained via [rememberPermissionState].
 *
 * [status] updates reactively — not just after [launchPermissionRequest], but whenever the
 * permission changes for any reason, including the user granting or revoking it from system
 * Settings while this composable is still on screen.
 */
@Stable
public interface PermissionState {
    public val permission: Permission
    public val status: PermissionStatus

    /** Requests [permission], suspending in the background; recompose happens via [status]. */
    public fun launchPermissionRequest()
}

private class MutablePermissionState(
    override val permission: Permission,
    private val manager: PermissionManager,
    private val scope: CoroutineScope,
) : PermissionState {

    var statusState by mutableStateOf(manager.check(permission))

    override val status: PermissionStatus get() = statusState

    override fun launchPermissionRequest() {
        scope.launch { manager.request(permission) }
    }
}

/**
 * Remembers a [PermissionState] for [permission], kept up to date while in composition.
 *
 * @throws io.github.ahmadhussain0335.kmppermission.PermissionException if [permission] is unsupported on this platform or
 *   missing its manifest/Info.plist entry. These are configuration errors and fail fast, as they do
 *   in the core API.
 */
@Composable
public fun rememberPermissionState(permission: Permission): PermissionState {
    val manager = rememberPermissionManager()
    val scope = rememberCoroutineScope()
    val state = remember(permission, manager) { MutablePermissionState(permission, manager, scope) }

    LaunchedEffect(state) {
        manager.observe(permission).collect { state.statusState = it }
    }

    return state
}
