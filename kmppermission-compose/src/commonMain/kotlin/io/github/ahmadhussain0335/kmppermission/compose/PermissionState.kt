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
import io.github.ahmadhussain0335.kmppermission.PermissionException
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

    /**
     * The configuration error from the last check, request or observation, such as a missing
     * manifest or Info.plist entry or a permission unsupported on this platform. `null` when there
     * is none. While set, [status] keeps its last known value ([PermissionStatus.NotDetermined]
     * if nothing could be read).
     */
    public val error: PermissionException?

    /** Requests [permission], suspending in the background; recompose happens via [status]. */
    public fun launchPermissionRequest()
}

internal class MutablePermissionState(
    override val permission: Permission,
    private val manager: PermissionManager,
    private val scope: CoroutineScope,
) : PermissionState {

    override var error: PermissionException? by mutableStateOf(null)

    override var status: PermissionStatus by mutableStateOf(
        capturingErrors { manager.check(permission) } ?: PermissionStatus.NotDetermined,
    )

    override fun launchPermissionRequest() {
        scope.launch {
            if (capturingErrors { manager.request(permission) } != null) error = null
        }
    }

    /** Runs [block], recording a [PermissionException] in [error] instead of throwing it. */
    inline fun <T : Any> capturingErrors(block: () -> T): T? =
        try {
            block()
        } catch (e: PermissionException) {
            error = e
            null
        }
}

/**
 * Remembers a [PermissionState] for [permission], kept up to date while in composition.
 *
 * Configuration errors (a missing manifest or Info.plist entry, or a permission unsupported on this
 * platform) do not crash the composition; they are reported through [PermissionState.error].
 */
@Composable
public fun rememberPermissionState(permission: Permission): PermissionState =
    rememberPermissionState(permission, rememberPermissionManager())

@Composable
internal fun rememberPermissionState(permission: Permission, manager: PermissionManager): PermissionState {
    val scope = rememberCoroutineScope()
    val state = remember(permission, manager) { MutablePermissionState(permission, manager, scope) }

    LaunchedEffect(state) {
        state.capturingErrors {
            manager.observe(permission).collect { state.status = it }
        }
    }

    return state
}
