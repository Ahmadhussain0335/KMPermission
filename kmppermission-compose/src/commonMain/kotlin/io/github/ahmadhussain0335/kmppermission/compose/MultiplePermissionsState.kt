package io.github.ahmadhussain0335.kmppermission.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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

/** Observable Compose state for a batch of [Permission]s, obtained via [rememberMultiplePermissions]. */
@Stable
public interface MultiplePermissionsState {
    public val permissions: List<PermissionState>

    /** True if every permission is [PermissionStatus.Granted]; [PermissionStatus.Limited] does not count. */
    public val allGranted: Boolean

    /** True if at least one permission is [PermissionStatus.Granted]; [PermissionStatus.Limited] does not count. */
    public val anyGranted: Boolean

    /**
     * The configuration error from the last batch request, or else the first error among
     * [permissions]; `null` when there is none. See [PermissionState.error].
     */
    public val error: PermissionException?

    /** Requests every permission in [permissions] in a single batched system prompt where supported. */
    public fun launchMultiplePermissionRequest()
}

internal class MutableMultiplePermissionsState(
    override val permissions: List<PermissionState>,
    private val manager: PermissionManager,
    private val scope: CoroutineScope,
) : MultiplePermissionsState {

    private var requestError: PermissionException? by mutableStateOf(null)

    override val allGranted: Boolean get() = permissions.all { it.status == PermissionStatus.Granted }
    override val anyGranted: Boolean get() = permissions.any { it.status == PermissionStatus.Granted }
    override val error: PermissionException? get() = requestError ?: permissions.firstNotNullOfOrNull { it.error }

    override fun launchMultiplePermissionRequest() {
        val requested = permissions.map(PermissionState::permission).toTypedArray()
        scope.launch {
            requestError = try {
                manager.request(*requested)
                null
            } catch (e: PermissionException) {
                e
            }
        }
    }
}

/**
 * Remembers a [MultiplePermissionsState] for [permissions].
 *
 * Configuration errors do not crash the composition; they are reported through
 * [MultiplePermissionsState.error].
 */
@Composable
public fun rememberMultiplePermissions(vararg permissions: Permission): MultiplePermissionsState =
    rememberMultiplePermissions(permissions.asList(), rememberPermissionManager())

@Composable
internal fun rememberMultiplePermissions(permissions: List<Permission>, manager: PermissionManager): MultiplePermissionsState {
    val scope = rememberCoroutineScope()
    // Keyed so each permission keeps its own state if the set changes between compositions.
    val permissionStates = permissions.map { key(it) { rememberPermissionState(it, manager) } }
    return remember(permissionStates, manager) { MutableMultiplePermissionsState(permissionStates, manager, scope) }
}
