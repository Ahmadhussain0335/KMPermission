package com.kmp.permission

/**
 * Logical category a [Permission] belongs to.
 *
 * Groups exist so host apps can present rationale UI, settings deep-links, or analytics by feature
 * area ("Location") instead of by exact permission ("fine vs. coarse vs. background location").
 */
enum class PermissionGroup {
    Camera,
    Microphone,
    Bluetooth,
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
