package com.kmp.permission

import kotlinx.coroutines.flow.Flow

/**
 * Reactive view over [Permission] state. Implemented by [PermissionManager]; kept as its own
 * interface so code that only needs to *watch* permissions (e.g. a settings screen reacting to the
 * user granting access in system Settings) doesn't need to depend on the request-capable surface.
 */
interface PermissionObserver {

    /**
     * A [Flow] that emits [permission]'s current [PermissionStatus] immediately on collection, then
     * again whenever it changes — including changes made outside the app, such as the user granting
     * or revoking access from system Settings while this app is backgrounded.
     */
    fun observe(permission: Permission): Flow<PermissionStatus>

    /**
     * Like [observe] but for several permissions at once, emitting the full [permissions] → status
     * map whenever any one of them changes.
     */
    fun observe(vararg permissions: Permission): Flow<Map<Permission, PermissionStatus>>
}
