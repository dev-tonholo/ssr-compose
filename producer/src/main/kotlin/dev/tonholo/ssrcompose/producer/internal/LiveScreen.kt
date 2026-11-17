package dev.tonholo.ssrcompose.producer.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import dev.tonholo.ssrcompose.core.message.ScreenContent
import dev.tonholo.ssrcompose.producer.internal.content.toScreenContent
import dev.tonholo.ssrcompose.producer.internal.frame.ImmediateFrameClock
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/**
 * One screen of one session, kept alive while something collects [contents].
 *
 * @param content the screen, as the server developer wrote it.
 * @param dispatcher runs the composition. It must run one thing at a time, as a composition is not thread safe.
 * @param notifier tells the screen about state writes.
 */
internal class LiveScreen(
    private val content: @Composable () -> Unit,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default.limitedParallelism(parallelism = 1),
    private val notifier: SnapshotNotifier = SnapshotNotifier.shared,
) {
    /**
     * Composes the screen for each collector and emits its content, then again whenever a state it reads changes
     * and the content differs from the last one sent. A collector that falls behind gets the latest content only.
     * The composition is disposed when the collector cancels.
     */
    fun contents(): Flow<ScreenContent> =
        channelFlow { compose() }
            .buffer(Channel.CONFLATED)
            .flowOn(dispatcher)
            .distinctUntilChanged()

    private suspend fun ProducerScope<ScreenContent>.compose() {
        val root = PlacedNode.Root()
        val recomposer = Recomposer(coroutineContext)
        val composition = Composition(
            applier = ScreenApplier(root = root, onChangesApplied = { trySend(root.toScreenContent()) }),
            parent = recomposer,
        )
        launch(start = CoroutineStart.UNDISPATCHED) { notifier.applyWritesWhileWaiting() }
        launch(context = ImmediateFrameClock, start = CoroutineStart.UNDISPATCHED) {
            recomposer.runRecomposeAndApplyChanges()
        }
        composition.setContent(content)
        awaitClose {
            composition.dispose()
            recomposer.cancel()
        }
    }
}
