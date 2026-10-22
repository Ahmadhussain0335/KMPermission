package com.kmp.permission

import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.kmp.permission.internal.PermissionController
import com.kmp.permission.internal.PermissionFactory
import kotlinx.coroutines.CoroutineScope

/**
 * Creates a [PermissionManager] bound to [activity].
 *
 * Safe to call at any point in the Activity's lifecycle — including from within Compose composition,
 * which typically runs well after `onCreate`. The returned instance is scoped to this specific
 * Activity instance: construct a new one after each recreation (e.g. a configuration change) rather
 * than retaining it across Activity instances.
 *
 * @param scope requests launched via the callback [PermissionManager.request] overloads run on this
 *   scope; defaults to [activity]'s own `lifecycleScope` so in-flight requests are cancelled
 *   automatically when the Activity is destroyed.
 */
fun PermissionManager(
    activity: ComponentActivity,
    scope: CoroutineScope = activity.lifecycleScope,
): PermissionManager {
    val controller = PermissionController()
    controller.attach(activity)
    return PermissionFactory.create(controller, scope)
}
