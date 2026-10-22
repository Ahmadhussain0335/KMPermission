package com.kmp.permission.compose

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import com.kmp.permission.Permission
import com.kmp.permission.PermissionStatus

/**
 * A ready-made [Button] that requests [permission] on click and reports [onGranted] whenever the
 * permission becomes [PermissionStatus.Granted] — including if it was already granted before this
 * composable first appeared.
 */
@Composable
fun PermissionButton(
    permission: Permission,
    onGranted: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    content: @Composable RowScope.() -> Unit,
) {
    val state = rememberPermissionState(permission)
    val currentOnGranted by rememberUpdatedState(onGranted)

    LaunchedEffect(state.status) {
        if (state.status == PermissionStatus.Granted) currentOnGranted()
    }

    Button(
        onClick = state::launchPermissionRequest,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        content = content,
    )
}
