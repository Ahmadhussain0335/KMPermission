package com.kmp.permission.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
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
    val allGranted: Boolean
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

@Composable
fun rememberMultiplePermissions(vararg permissions: Permission): MultiplePermissionsState {
    val manager = rememberPermissionManager()
    val scope = rememberCoroutineScope()
    val permissionStates = permissions.map { rememberPermissionState(it) }
    return remember(permissionStates, manager) { MutableMultiplePermissionsState(permissionStates, manager, scope) }
}
