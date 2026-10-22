package io.github.ahmadhussain0335.kmppermission.compose

import androidx.compose.runtime.Composable
import io.github.ahmadhussain0335.kmppermission.PermissionManager

/**
 * Obtains a [PermissionManager] scoped correctly for the current composition, created
 * automatically — Android binds it to the current Activity (via [androidx.compose.ui.platform.LocalContext]),
 * iOS to a composition-scoped [kotlinx.coroutines.CoroutineScope]. Every public API in this module is
 * built on top of this; application code never constructs a [PermissionManager] itself when using
 * Compose.
 */
@Composable
internal expect fun rememberPermissionManager(): PermissionManager
