package io.github.ahmadhussain0335.kmppermission.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import io.github.ahmadhussain0335.kmppermission.Permission
import io.github.ahmadhussain0335.kmppermission.PermissionStatus

/**
 * A clickable area that requests [permission] on click and reports [onGranted] whenever the
 * permission becomes [PermissionStatus.Granted] — including if it was already granted before this
 * composable first appeared.
 *
 * It draws nothing itself and has the accessibility role of a button, so it works with any design
 * system: put your own styled content inside, or apply your button's styling through [modifier].
 */
@Composable
public fun PermissionButton(
    permission: Permission,
    onGranted: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberPermissionState(permission)
    val currentOnGranted by rememberUpdatedState(onGranted)

    LaunchedEffect(state.status) {
        if (state.status == PermissionStatus.Granted) currentOnGranted()
    }

    Box(
        modifier = modifier.clickable(enabled = enabled, role = Role.Button, onClick = state::launchPermissionRequest),
        content = content,
    )
}
