package com.kmp.permission.sample.android

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kmp.permission.PermissionStatus

@Composable
fun StatusBadge(status: PermissionStatus, modifier: Modifier = Modifier) {
    val (label, color) = when (status) {
        PermissionStatus.Granted -> "Granted" to Color(0xFF2E7D32)
        PermissionStatus.Denied -> "Denied" to Color(0xFFEF6C00)
        PermissionStatus.PermanentlyDenied -> "Blocked — use Settings" to Color(0xFFC62828)
        PermissionStatus.Restricted -> "Restricted" to Color(0xFF616161)
        PermissionStatus.NotDetermined -> "Not requested yet" to Color(0xFF546E7A)
    }

    Surface(color = color, contentColor = Color.White, shape = MaterialTheme.shapes.small, modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

/** Worst-case status across a group of permissions requested together (e.g. Bluetooth's three sub-permissions). */
fun worstStatus(statuses: List<PermissionStatus>): PermissionStatus = when {
    statuses.any { it == PermissionStatus.PermanentlyDenied } -> PermissionStatus.PermanentlyDenied
    statuses.any { it == PermissionStatus.Restricted } -> PermissionStatus.Restricted
    statuses.any { it == PermissionStatus.Denied } -> PermissionStatus.Denied
    statuses.any { it == PermissionStatus.NotDetermined } -> PermissionStatus.NotDetermined
    else -> PermissionStatus.Granted
}
