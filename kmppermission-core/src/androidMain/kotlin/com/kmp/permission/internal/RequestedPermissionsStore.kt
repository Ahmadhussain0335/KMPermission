package com.kmp.permission.internal

import android.content.Context
import com.kmp.permission.Permission

/** What the library has observed about past requests for one [Permission] since it was last granted. */
internal data class RequestHistory(
    /** A request completed without a grant. */
    val requested: Boolean = false,
    /** `shouldShowRequestPermissionRationale` returned `true`, so the user has denied it before. */
    val rationaleSeen: Boolean = false,
    /** A request was answered by the system without showing a dialog. */
    val autoDenied: Boolean = false,
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

    /** Applies [transform] to [permission]'s history atomically and returns the stored result. */
    fun update(permission: Permission, transform: (RequestHistory) -> RequestHistory): RequestHistory =
        synchronized(LOCK) {
            val current = read(permission)
            val updated = transform(current)
            if (updated != current) write(permission, updated)
            updated
        }

    private fun read(permission: Permission): RequestHistory {
        val id = PermissionMapper.stableId(permission)
        return RequestHistory(
            requested = prefs.getBoolean("$id$REQUESTED_SUFFIX", false),
            rationaleSeen = prefs.getBoolean("$id$RATIONALE_SEEN_SUFFIX", false),
            autoDenied = prefs.getBoolean("$id$AUTO_DENIED_SUFFIX", false),
        )
    }

    private fun write(permission: Permission, history: RequestHistory) {
        val id = PermissionMapper.stableId(permission)
        prefs.edit()
            .putBoolean("$id$REQUESTED_SUFFIX", history.requested)
            .putBoolean("$id$RATIONALE_SEEN_SUFFIX", history.rationaleSeen)
            .putBoolean("$id$AUTO_DENIED_SUFFIX", history.autoDenied)
            .apply()
    }

    companion object {
        const val PREFS_NAME = "com.kmp.permission.requested_permissions"
        private const val REQUESTED_SUFFIX = ".requested"
        private const val RATIONALE_SEEN_SUFFIX = ".rationale_seen"
        private const val AUTO_DENIED_SUFFIX = ".auto_denied"

        // Shared by every store instance: managers bound to different Activities use the same file.
        private val LOCK = Any()
    }
}
