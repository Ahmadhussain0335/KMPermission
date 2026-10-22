package io.github.ahmadhussain0335.kmppermission.internal

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withTimeoutOrNull
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationState
import platform.UIKit.UIApplicationWillResignActiveNotification
import kotlin.time.Duration

internal enum class AppActivityEvent { WillResignActive, DidBecomeActive }

/**
 * Subscribes to the app leaving and re-entering the active state, delivered on the main queue.
 *
 * A system permission alert moves the app out of the active state while it is on screen, which is
 * the only signal iOS gives for prompts that can be dismissed without changing authorization.
 *
 * @return a function that removes the observers; call it exactly once.
 */
internal fun observeAppActivity(onEvent: (AppActivityEvent) -> Unit): () -> Unit {
    val center = NSNotificationCenter.defaultCenter
    val tokens = listOf(
        UIApplicationWillResignActiveNotification to AppActivityEvent.WillResignActive,
        UIApplicationDidBecomeActiveNotification to AppActivityEvent.DidBecomeActive,
    ).map { (name, event) ->
        center.addObserverForName(name, `object` = null, queue = NSOperationQueue.mainQueue) { _ -> onEvent(event) }
    }
    return { tokens.forEach { center.removeObserver(it) } }
}

/**
 * Suspends until the app is active, for at most [timeout]. Must be called on the main thread.
 *
 * A system alert presented while the app is still inactive (for instance right after another
 * alert closed) produces no WillResignActive, so prompt detection has to start from the active state.
 */
internal suspend fun awaitAppActive(timeout: Duration) {
    val becameActive = Channel<Unit>(Channel.CONFLATED)
    val removeObservers = observeAppActivity { if (it == AppActivityEvent.DidBecomeActive) becameActive.trySend(Unit) }
    try {
        if (UIApplication.sharedApplication.applicationState != UIApplicationState.UIApplicationStateActive) {
            withTimeoutOrNull(timeout) { becameActive.receive() }
        }
    } finally {
        removeObservers()
    }
}
