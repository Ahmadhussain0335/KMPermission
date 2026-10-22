package io.github.ahmadhussain0335.kmppermission.internal

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.Foundation.NSProcessInfo

/** Major version of the iOS release the app is running on, e.g. `18`. */
@OptIn(ExperimentalForeignApi::class)
internal val iosMajorVersion: Int by lazy {
    NSProcessInfo.processInfo.operatingSystemVersion.useContents { majorVersion.toInt() }
}
