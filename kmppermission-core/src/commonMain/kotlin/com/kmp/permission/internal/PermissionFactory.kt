package com.kmp.permission.internal

import com.kmp.permission.PermissionManager
import kotlinx.coroutines.CoroutineScope

/**
 * Assembles a [PermissionManager] from a platform [PermissionEngine].
 *
 * The platform-specific piece — constructing that [PermissionEngine] itself, which on Android needs
 * a live `ComponentActivity` — is wired up by the public entry points added in Milestones 4 and 5.
 */
internal object PermissionFactory {

    fun create(engine: PermissionEngine, scope: CoroutineScope): PermissionManager =
        DefaultPermissionManager(engine, PermissionRegistry(), scope)
}
