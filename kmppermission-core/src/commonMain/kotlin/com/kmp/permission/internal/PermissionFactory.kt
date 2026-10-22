package com.kmp.permission.internal

import com.kmp.permission.PermissionManager
import kotlinx.coroutines.CoroutineScope

/**
 * Assembles a [PermissionManager] from a platform [PermissionEngine]. Called by the public
 * platform factories, which construct the engine itself (on Android that needs a live
 * `ComponentActivity`).
 */
internal object PermissionFactory {

    fun create(engine: PermissionEngine, scope: CoroutineScope): PermissionManager =
        DefaultPermissionManager(engine, PermissionRegistry(), scope)
}
