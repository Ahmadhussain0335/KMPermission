package io.github.ahmadhussain0335.kmppermission.internal

/** The platform this code is currently compiled/running for. Purely for cross-platform validation logic. */
internal enum class Platform(val displayName: String) {
    Android("Android"),
    Ios("iOS"),
}

/** The platform this code is currently running on. */
internal expect val currentPlatform: Platform
