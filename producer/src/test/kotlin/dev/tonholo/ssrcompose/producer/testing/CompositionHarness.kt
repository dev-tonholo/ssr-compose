package dev.tonholo.ssrcompose.producer.testing

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import dev.tonholo.ssrcompose.producer.internal.PlacedNode
import dev.tonholo.ssrcompose.producer.internal.ScreenApplier
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent

internal fun composeOnce(content: @Composable () -> Unit): PlacedNode.Root {
    val root = PlacedNode.Root()
    val recomposer = Recomposer(EmptyCoroutineContext)
    val composition = Composition(ScreenApplier(root), recomposer)
    composition.setContent(content)
    recomposer.cancel()
    return root
}

@OptIn(ExperimentalCoroutinesApi::class)
internal class LiveComposition(
    private val scope: TestScope,
    content: @Composable () -> Unit,
) {
    val root = PlacedNode.Root()
    private val clock = BroadcastFrameClock()
    private val recomposer = Recomposer(scope.backgroundScope.coroutineContext)
    private val composition = Composition(ScreenApplier(root), recomposer)
    private var frameTime = 0L

    init {
        scope.backgroundScope.launch(clock) { recomposer.runRecomposeAndApplyChanges() }
        scope.runCurrent()
        composition.setContent(content)
        scope.runCurrent()
    }

    fun recompose() {
        Snapshot.sendApplyNotifications()
        scope.runCurrent()
        clock.sendFrame(frameTime++)
        scope.runCurrent()
    }

    fun dispose() {
        composition.dispose()
        recomposer.cancel()
    }
}
