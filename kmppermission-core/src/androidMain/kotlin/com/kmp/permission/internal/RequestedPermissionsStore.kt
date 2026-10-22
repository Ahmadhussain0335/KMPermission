package com.kmp.permission.internal

import android.content.Context
import com.kmp.permission.Permission

/** What the library has observed about past requests for one [Permission]. */
internal data class RequestHistory(
    /** Completed requests that ended without a grant since the permission was last seen granted. */
    val deniedCount: Int = 0,
    /** Whether `shouldShowRequestPermissionRationale` has returned `true` since then. */
    val rationaleSeen: Boolean = false,
)

/**
 * Remembers each [Permission]'s [RequestHistory].
 *
 * Android's `shouldShowRequestPermissionRationale` returns `false` the first time a permission is
 * checked (never asked), after a dismissed dialog, and after a permanent denial. Telling those apart
 * needs a memory of what happened before, which the platform does not keep for us.
 *
 * Host apps that back up SharedPreferences should exclude [PREFS_NAME]: restored history on a new
 * device, where the permissions start ungranted, would mark them as permanently denied.
 */
internal class RequestedPermissionsStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun history(permission: Permission): RequestHistory {
        val id = PermissionMapper.stableId(permission)
        return RequestHistory(
            deniedCount = prefs.getInt("$id$DENIED_COUNT_SUFFIX", 0),
            rationaleSeen = prefs.getBoolean("$id$RATIONALE_SEEN_SUFFIX", false),
        )
    }

    fun save(permission: Permission, history: RequestHistory) {
        if (history == history(permission)) return
        val id = PermissionMapper.stableId(permission)
        prefs.edit()
            .putInt("$id$DENIED_COUNT_SUFFIX", history.deniedCount)
            .putBoolean("$id$RATIONALE_SEEN_SUFFIX", history.rationaleSeen)
            .apply()
    }

    companion object {
        const val PREFS_NAME = "com.kmp.permission.requested_permissions"
        private const val DENIED_COUNT_SUFFIX = ".denied_count"
        private const val RATIONALE_SEEN_SUFFIX = ".rationale_seen"
    }
}
