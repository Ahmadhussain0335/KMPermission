package com.kmp.permission.internal

import android.content.Context
import com.kmp.permission.Permission

/**
 * Remembers whether each [Permission] has ever been requested from this app.
 *
 * Android's `shouldShowRequestPermissionRationale` returns `false` both the very first time a
 * permission is checked (never asked) and after it's been permanently denied — the only way to tell
 * those two states apart is to remember, ourselves, whether we've asked before.
 */
internal class RequestedPermissionsStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun wasRequested(permission: Permission): Boolean = prefs.getBoolean(keyFor(permission), false)

    fun markRequested(permission: Permission) {
        prefs.edit().putBoolean(keyFor(permission), true).apply()
    }

    private fun keyFor(permission: Permission): String = permission::class.simpleName ?: permission.toString()

    private companion object {
        const val PREFS_NAME = "com.kmp.permission.requested_permissions"
    }
}
