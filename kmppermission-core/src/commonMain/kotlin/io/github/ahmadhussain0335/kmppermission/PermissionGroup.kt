package io.github.ahmadhussain0335.kmppermission

/**
 * Logical category a [Permission] belongs to.
 *
 * Groups exist so host apps can present rationale UI, settings deep-links, or analytics by feature
 * area ("Location") instead of by exact permission ("fine vs. coarse vs. background location").
 */
public enum class PermissionGroup {
    Camera,
    Microphone,
    Bluetooth,
    /** Wi-Fi discovery and local network access. */
    Network,
    Location,
    Notifications,
    Contacts,
    Calendar,
    Storage,
    Photos,
    Nfc,
    Motion,
    SpeechRecognition,
}
