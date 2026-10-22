package io.github.ahmadhussain0335.kmppermission

import io.github.ahmadhussain0335.kmppermission.internal.PermissionController
import io.github.ahmadhussain0335.kmppermission.internal.PermissionFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope

/**
 * Creates a [PermissionManager] for use anywhere in the app.
 *
 * Unlike the Android factory, this needs no host reference — every native permission API this
 * library talks to on iOS (AVFoundation, CoreLocation, CoreBluetooth, etc.) is process-wide rather
 * than tied to a particular view controller.
 *
 * @param scope requests launched via the callback [PermissionManager.request] overloads run on this
 *   scope; defaults to a fresh main-thread [MainScope].
 */
public fun PermissionManager(scope: CoroutineScope = MainScope()): PermissionManager =
    PermissionFactory.create(PermissionController(), scope)

/**
 * Convenience overload for Swift/Objective-C callers — default parameter values aren't visible
 * across the Kotlin/Native interop boundary, so [PermissionManager]'s default [MainScope] isn't
 * reachable from Swift without supplying a [CoroutineScope] explicitly. This is the equivalent of
 * what `@JvmOverloads` generates automatically on the JVM, written by hand for Kotlin/Native.
 */
public fun PermissionManager(): PermissionManager = PermissionManager(MainScope())
