package com.kmp.permission

/**
 * The single entry point application code uses to check, request, and observe runtime permissions
 * across Android and iOS.
 *
 * All platform detail — Android's Activity Result API, iOS's AVFoundation/CoreLocation/etc. — is
 * hidden behind this interface. Application code declares the underlying platform permissions in
 * `AndroidManifest.xml` / `Info.plist` as usual; everything else — showing the system prompt,
 * interpreting the result, detecting a permanent denial, reacting to a Settings-app change — is
 * handled here.
 *
 * A concrete instance is obtained from the platform factory: `PermissionManager(activity)` on
 * Android, `PermissionManager()` on iOS. Compose code uses the `kmppermission-compose` functions
 * instead, which create one automatically.
 */
interface PermissionManager : PermissionObserver {

    /** Requests [permission], suspending until the user responds (or immediately if already decided). */
    suspend fun request(permission: Permission): PermissionResult

    /**
     * Requests all of [permissions] in a single system prompt where the platform supports batching,
     * suspending until every one of them has been resolved.
     */
    suspend fun request(vararg permissions: Permission): Map<Permission, PermissionResult>

    /** Callback variant of [request] for call sites that are not in a coroutine. */
    fun request(permission: Permission, onResult: (PermissionResult) -> Unit)

    /**
     * Callback variant of the batch [request] for call sites that are not in a coroutine.
     *
     * Takes a [List] rather than `vararg` — combined with a trailing lambda, a `vararg` overload
     * here is genuinely ambiguous with the single-permission [request] overload above (Kotlin can't
     * tell `request(Permission.Camera) { ... }` apart from a one-element batch until it's too late).
     */
    fun request(permissions: List<Permission>, onResult: (Map<Permission, PermissionResult>) -> Unit)

    /** Returns [permission]'s current status without prompting the user. */
    fun check(permission: Permission): PermissionStatus

    /** Returns `true` only if every one of [permissions] is currently [PermissionStatus.Granted]. */
    fun areGranted(vararg permissions: Permission): Boolean

    /**
     * Opens the platform's own app settings screen — the only way for a user to reverse a
     * [PermissionStatus.PermanentlyDenied] permission.
     */
    fun openSettings()
}
