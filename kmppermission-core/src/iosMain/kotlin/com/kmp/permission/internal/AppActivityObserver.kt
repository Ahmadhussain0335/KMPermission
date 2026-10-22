package com.kmp.permission.internal

import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationWillResignActiveNotification

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
