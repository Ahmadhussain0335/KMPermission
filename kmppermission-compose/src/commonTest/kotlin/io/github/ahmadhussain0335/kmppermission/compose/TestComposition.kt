package io.github.ahmadhussain0335.kmppermission.compose

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent

/** An applier for compositions that emit no nodes, which is all the state holders need. */
private class NoNodeApplier : AbstractApplier<Unit>(Unit) {
    override fun insertTopDown(index: Int, instance: Unit) = Unit
    override fun insertBottomUp(index: Int, instance: Unit) = Unit
    override fun remove(index: Int, count: Int) = Unit
    override fun move(from: Int, to: Int, count: Int) = Unit
    override fun onClear() = Unit
}

/**
 * Composes [content] in a bare Compose runtime driven by this [TestScope]'s scheduler, runs [test],
 * then disposes the composition. Effects and `rememberCoroutineScope` launches run on the test
 * dispatcher, so `runCurrent()` and `advanceUntilIdle()` drive them deterministically. No UI, so it
 * behaves the same on the Android host and on iOS.
 */
internal fun TestScope.withComposition(content: @Composable () -> Unit, test: () -> Unit) {
    val frameClock = BroadcastFrameClock()
    val recomposer = Recomposer(coroutineContext + frameClock)
    val recomposerJob = launch(frameClock) { recomposer.runRecomposeAndApplyChanges() }
    val composition = Composition(NoNodeApplier(), recomposer)
    try {
        composition.setContent(content)
        runCurrent()
        test()
    } finally {
        composition.dispose()
        recomposer.cancel()
        recomposerJob.cancel()
    }
}
