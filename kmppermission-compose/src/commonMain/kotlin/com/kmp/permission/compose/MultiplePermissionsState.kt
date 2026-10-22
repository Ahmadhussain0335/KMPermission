package com.kmp.permission.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.kmp.permission.Permission
import com.kmp.permission.PermissionManager
import com.kmp.permission.PermissionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** Observable Compose state for a batch of [Permission]s, obtained via [rememberMultiplePermissions]. */
@Stable
interface MultiplePermissionsState {
    val permissions: List<PermissionState>

    /** True if every permission is [PermissionStatus.Granted]; [PermissionStatus.Limited] does not count. */
    val allGranted: Boolean

    /** True if at least one permission is [PermissionStatus.Granted]; [PermissionStatus.Limited] does not count. */
    val anyGranted: Boolean

    /** Requests every permission in [permissions] in a single batched system prompt where supported. */
    fun launchMultiplePermissionRequest()
}

private class MutableMultiplePermissionsState(
    override val permissions: List<PermissionState>,
    private val manager: PermissionManager,
    private val scope: CoroutineScope,
) : MultiplePermissionsState {

    override val allGranted: Boolean get() = permissions.all { it.status == PermissionStatus.Granted }
    override val anyGranted: Boolean get() = permissions.any { it.status == PermissionStatus.Granted }

    override fun launchMultiplePermissionRequest() {
        val requested = permissions.map(PermissionState::permission).toTypedArray()
        scope.launch { manager.request(*requested) }
    }
}

/**
 * Remembers a [MultiplePermissionsState] for [permissions].
 *
 * @throws com.kmp.permission.PermissionException if a permission is unsupported on this platform or
 *   missing its manifest/Info.plist entry. These are configuration errors and fail fast, as they do
 *   in the core API.
 */
@Composable
fun rememberMultiplePermissions(vararg permissions: Permission): MultiplePermissionsState {
    val manager = rememberPermissionManager()
    val scope = rememberCoroutineScope()
    // Keyed so each permission keeps its own state if the set changes between compositions.
    val permissionStates = permissions.map { key(it) { rememberPermissionState(it) } }
    return remember(permissionStates, manager) { MutableMultiplePermissionsState(permissionStates, manager, scope) }
}
