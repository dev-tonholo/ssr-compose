package dev.tonholo.ssrcompose.producer.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import dev.tonholo.ssrcompose.producer.internal.reader.ScreenRead
import dev.tonholo.ssrcompose.producer.internal.reader.readScreen
import dev.tonholo.ssrcompose.producer.internal.scene.SceneSetup
import dev.tonholo.ssrcompose.producer.internal.scene.ScreenScene
import dev.tonholo.ssrcompose.producer.internal.scene.sceneDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

/**
 * One screen of one session, hosted in a scene while something collects [frames].
 *
 * @param content the screen, as the server developer wrote it.
 * @param setup the size, density and direction the scene lays the screen out with.
 * @param dispatcher creates, drives and closes the scene, and runs its effects. By default the producer's one scene
 * thread; a test may pass its own, which must run one thing at a time.
 * @param notifier tells the screen about state writes.
 * @param revisions numbers the documents sent.
 * @param registerApplyObserver starts watching applied state changes and returns the handle that stops it.
 */
internal class LiveScreen(
    private val content: @Composable () -> Unit,
    private val setup: SceneSetup,
    private val dispatcher: CoroutineDispatcher = sceneDispatcher,
    private val notifier: SnapshotNotifier = SnapshotNotifier.shared,
    private val revisions: Revisions = Revisions(),
    private val registerApplyObserver: ((Set<Any>, Snapshot) -> Unit) -> ObserverHandle = { observer ->
        Snapshot.registerApplyObserver(observer = observer)
    },
) {
    /**
     * Lays the screen out for each collector and emits what it shows, then again whenever a state it reads changes
     * and what it shows differs from the last frame. A collector that falls behind gets the latest frame only.
     * The flow fails with the error of a composition or an effect that throws. The scene is closed when the flow
     * ends, however it ends.
     */
    fun frames(): Flow<ScreenFrame> =
        channelFlow { host() }
            .buffer(Channel.CONFLATED)
            .flowOn(dispatcher)

    private suspend fun ProducerScope<ScreenFrame>.host() {
        val changes = Channel<Unit>(capacity = Channel.CONFLATED)
        launch(start = CoroutineStart.UNDISPATCHED) { notifier.applyWritesWhileWaiting() }
        val failEffects = CoroutineExceptionHandler { _, error -> changes.close(cause = error) }
        ScreenScene(setup = setup, coroutineContext = dispatcher + failEffects).use { scene ->
            val applyObserver = registerApplyObserver { _, _ -> changes.trySend(Unit) }
            try {
                scene.setContent(content = content)
                sendChanges(scene = scene, changes = changes)
            } finally {
                applyObserver.dispose()
            }
        }
    }

    private suspend fun ProducerScope<ScreenFrame>.sendChanges(scene: ScreenScene, changes: Channel<Unit>): Nothing {
        var lastRead: ScreenRead? = null
        while (true) {
            scene.render()
            val read = scene.read { roots -> readScreen(roots = roots, density = scene.density) }
            if (read != lastRead) send(read.toFrame())
            lastRead = read
            do changes.receive() while (!scene.hasInvalidations())
        }
    }

    private fun ScreenRead.toFrame(): ScreenFrame = when (this) {
        is ScreenRead.Document -> ScreenFrame.Sent(revision = revisions.next(), document = document)
        is ScreenRead.NotSendable -> ScreenFrame.NotSendable(reason = reason)
    }
}
