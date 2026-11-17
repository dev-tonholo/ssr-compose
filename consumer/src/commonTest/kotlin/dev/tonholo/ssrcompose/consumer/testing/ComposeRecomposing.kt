package dev.tonholo.ssrcompose.consumer.testing

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

/**
 * Composes [content] without a UI and runs [interact] with a function that applies pending state changes and
 * recomposes. The composition is disposed afterwards.
 */
fun composeAndRecompose(content: @Composable () -> Unit, interact: (recompose: () -> Unit) -> Unit) = runTest {
    val clock = BroadcastFrameClock()
    val recomposer = Recomposer(effectCoroutineContext = coroutineContext + clock)
    val recomposing = launch(context = clock) { recomposer.runRecomposeAndApplyChanges() }
    val composition = Composition(applier = NoopApplier(), parent = recomposer)
    try {
        composition.setContent(content = content)
        interact {
            Snapshot.sendApplyNotifications()
            testScheduler.runCurrent()
            clock.sendFrame(timeNanos = 0L)
            testScheduler.runCurrent()
        }
    } finally {
        composition.dispose()
        recomposer.cancel()
        recomposing.cancel()
    }
}
