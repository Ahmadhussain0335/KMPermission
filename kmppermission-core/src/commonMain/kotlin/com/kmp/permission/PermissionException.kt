package com.kmp.permission

/**
 * Thrown when the library cannot honor a permission request or check because the host app is
 * missing required platform configuration, or because the requested [Permission] does not exist on
 * the current platform.
 *
 * These are developer errors, not user-facing denials — a real user denial is represented by
 * [PermissionStatus.Denied] / [PermissionStatus.PermanentlyDenied], never by an exception.
 */
public sealed class PermissionException(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /**
     * The host app's `AndroidManifest.xml` is missing one or more `<uses-permission>` entries
     * required by [permission].
     */
    public class ManifestPermissionMissing(
        public val permission: Permission,
        public val manifestEntries: List<String>,
    ) : PermissionException(
        "Missing manifest declaration for ${permission::class.simpleName}. " +
            "Add the following to your AndroidManifest.xml:\n" +
            manifestEntries.joinToString("\n") { "<uses-permission android:name=\"$it\"/>" }
    )

    /**
     * The host app's `Info.plist` is missing the usage description key required by [permission].
     */
    public class InfoPlistUsageDescriptionMissing(
        public val permission: Permission,
        public val infoPlistKey: String,
    ) : PermissionException(
        "Missing Info.plist usage description for ${permission::class.simpleName}. " +
            "Add the key \"$infoPlistKey\" with a user-facing description string to your Info.plist."
    )

    /** [permission] does not exist on the current platform (e.g. requesting [Permission.Nfc] on iOS). */
    public class UnsupportedPermission(
        public val permission: Permission,
        public val platform: String,
    ) : PermissionException("${permission::class.simpleName} is not supported on $platform.")

    /**
     * A request was made while the [PermissionManager] has no live UI host to launch a system prompt
     * from (e.g. no foregrounded Activity on Android, no key window on iOS).
     */
    public class ManagerNotAttached(
        message: String = "PermissionManager has no active Activity/UIViewController to launch a " +
            "request from. Requests must be made while the host screen is in the foreground.",
    ) : PermissionException(message)
}
