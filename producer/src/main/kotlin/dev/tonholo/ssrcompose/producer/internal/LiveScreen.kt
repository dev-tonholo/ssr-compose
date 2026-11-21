package dev.tonholo.ssrcompose.producer.internal

import androidx.compose.runtime.Composable
import dev.tonholo.ssrcompose.producer.internal.reader.ScreenRead
import dev.tonholo.ssrcompose.producer.internal.reader.readScreen
import dev.tonholo.ssrcompose.producer.internal.scene.FrameTimeSource
import dev.tonholo.ssrcompose.producer.internal.scene.SceneSetup
import dev.tonholo.ssrcompose.producer.internal.scene.ScreenScene
import dev.tonholo.ssrcompose.producer.internal.scene.WakingDispatcher
import dev.tonholo.ssrcompose.producer.internal.scene.sceneDispatcher
import dev.tonholo.ssrcompose.producer.internal.text.ScaledTextScene
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch

private val FRAME_INTERVAL = 16.milliseconds

/**
 * One screen of one session, hosted in a scene while something collects [frames].
 *
 * @param content the screen, as the server developer wrote it.
 * @param setup the size, density and direction the scene lays the screen out with.
 * @param scaledTextScene the device's font scale curve and font scale, read in composition so a device change
 * recomposes the texts.
 * @param dispatcher creates, drives and closes the scene, and runs its effects. By default the producer's one scene
 * thread; a test may pass its own, which must run one thing at a time.
 * @param notifier tells the screen about state writes.
 * @param revisions numbers the documents sent.
 * @param frameTime gives the scene the time of each frame it renders.
 * @param framePause waits between two frames of a screen that keeps asking for frames, 16 ms by default.
 */
internal class LiveScreen(
    private val content: @Composable () -> Unit,
    private val setup: SceneSetup,
    private val scaledTextScene: () -> ScaledTextScene,
    private val dispatcher: CoroutineDispatcher = sceneDispatcher,
    private val notifier: SnapshotNotifier = SnapshotNotifier.shared,
    private val revisions: Revisions = Revisions(),
    private val frameTime: FrameTimeSource = FrameTimeSource.Monotonic,
    private val framePause: suspend () -> Unit = { delay(FRAME_INTERVAL) },
) {
    /**
     * Lays the screen out for each collector and emits what it shows, then again whenever a state it reads changes
     * or an effect asks for a frame, and what it shows differs from the last frame. While an effect keeps asking for
     * frames, such as an animation, the scene renders one frame per 16 ms. A collector that falls behind gets the
     * latest frame only.
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
        val wakingDispatcher = WakingDispatcher(delegate = dispatcher) { changes.trySend(Unit) }
        val sceneContext = wakingDispatcher + failEffects
        ScreenScene(
            setup = setup,
            coroutineContext = sceneContext,
            frameTime = frameTime,
            readScaledTextScene = scaledTextScene,
        ).use { scene ->
            scene.setContent(content = content)
            sendChanges(scene = scene, changes = changes)
        }
    }

    private suspend fun ProducerScope<ScreenFrame>.sendChanges(scene: ScreenScene, changes: Channel<Unit>): Nothing {
        var lastRead: ScreenRead? = null
        while (true) {
            scene.render()
            val read = scene.read { roots -> readScreen(roots = roots, density = scene.density) }
            if (read != lastRead) send(read.toFrame())
            lastRead = read
            awaitWork(scene = scene, changes = changes)
        }
    }

    private suspend fun awaitWork(scene: ScreenScene, changes: Channel<Unit>) {
        if (scene.hasInvalidations()) {
            framePause()
        } else {
            do changes.receive() while (!scene.hasInvalidations())
        }
    }

    private fun ScreenRead.toFrame(): ScreenFrame = when (this) {
        is ScreenRead.Document -> ScreenFrame.Sent(revision = revisions.next(), document = document)
        is ScreenRead.NotSendable -> ScreenFrame.NotSendable(reason = reason)
    }
}
