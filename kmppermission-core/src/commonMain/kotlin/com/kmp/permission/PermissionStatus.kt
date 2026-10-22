package com.kmp.permission

/**
 * The state of a [Permission] at a single point in time.
 *
 * Returned by [PermissionManager.check] and emitted by [PermissionObserver.observe]. See
 * [PermissionResult] for the equivalent type returned by [PermissionManager.request].
 */
sealed interface PermissionStatus {

    /** Granted; the guarded feature can be used immediately. */
    data object Granted : PermissionStatus

    /**
     * Partly granted: the feature works, but with reduced scope. Requesting again or opening
     * settings can widen it. Reported for:
     * - [Permission.Photos]: the user picked specific photos (iOS Limited, Android 14+ selected photos).
     * - [Permission.Contacts]: the user shared specific contacts (iOS 18+).
     * - [Permission.Calendar]: write-only access (iOS 17+).
     * - [Permission.LocationFine]: only approximate location was allowed.
     */
    data object Limited : PermissionStatus

    /** Denied, but the user can still be asked again — a rationale may help. */
    data object Denied : PermissionStatus

    /**
     * Denied, and the platform will no longer show its own request prompt. The only way forward is
     * [PermissionManager.openSettings].
     *
     * On Android this is a denial where `shouldShowRequestPermissionRationale` returns `false` after
     * a prior denial. On iOS it is the `denied` authorization status.
     */
    data object PermanentlyDenied : PermissionStatus

    /**
     * Unavailable for reasons outside the user's control — an MDM profile, parental controls, or
     * iOS's `restricted` authorization status. Requesting again or opening settings will not help.
     */
    data object Restricted : PermissionStatus

    /** The user has never been asked for this permission yet. */
    data object NotDetermined : PermissionStatus
}
