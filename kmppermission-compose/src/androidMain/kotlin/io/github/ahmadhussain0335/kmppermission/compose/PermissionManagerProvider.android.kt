package io.github.ahmadhussain0335.kmppermission.compose

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import io.github.ahmadhussain0335.kmppermission.PermissionManager
import java.lang.ref.WeakReference
import java.util.WeakHashMap

/**
 * Every [rememberPermissionState]/[rememberMultiplePermissions]/etc. call site independently calls
 * [rememberPermissionManager] — without sharing, each one would bind its own [PermissionManager] (and
 * therefore its own Activity Result launcher) to the same Activity. This cache keys by Activity so
 * they all share one. Both the key and the value are held weakly so neither this cache nor the
 * [PermissionManager] itself (which holds a reference back to the Activity) can keep an Activity
 * alive past its natural lifecycle.
 */
private val cache = WeakHashMap<ComponentActivity, WeakReference<PermissionManager>>()

@Composable
internal actual fun rememberPermissionManager(): PermissionManager {
    val activity = LocalContext.current.findActivity()
    return remember(activity) {
        cache[activity]?.get() ?: PermissionManager(activity).also { cache[activity] = WeakReference(it) }
    }
}

private tailrec fun Context.findActivity(): ComponentActivity = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> error("KMPermission Compose APIs require a ComponentActivity context, but none was found.")
}
