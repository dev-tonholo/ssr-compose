package dev.tonholo.ssrcompose.producer.memory

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.unit.dp
import dev.tonholo.ssrcompose.producer.internal.LiveScreen
import dev.tonholo.ssrcompose.producer.internal.ScreenFrame
import dev.tonholo.ssrcompose.producer.internal.compat.SceneHost
import dev.tonholo.ssrcompose.producer.internal.compat.openSceneHost
import dev.tonholo.ssrcompose.producer.internal.reader.ScreenRead
import dev.tonholo.ssrcompose.producer.internal.reader.readScreen
import dev.tonholo.ssrcompose.producer.internal.scene.withSceneLock
import dev.tonholo.ssrcompose.producer.internal.text.LocalScaledTextScene
import dev.tonholo.ssrcompose.producer.testing.liveScaledTextScene
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private const val SCREENS_PER_COLUMN = 1000
private const val SCREEN_WIDTH_DP = 360
private const val SCREEN_HEIGHT_DP = 1700

/** Screens held open at once, so that their memory can be read. */
internal interface MemorySessions : AutoCloseable {
    /** How many frames the open screens have rendered so far, or null for screens nothing renders again. */
    val framesRendered: Long? get() = null

    /** Opens screens until [count] are open or [mayOpenAfter] refuses one more, and returns how many are open. */
    fun open(count: Int, mayOpenAfter: (opened: Int) -> Boolean): Int
}

private abstract class OneByOneSessions : MemorySessions {
    private val held = mutableListOf<AutoCloseable>()

    protected abstract fun openOne(seed: Int): AutoCloseable

    override fun open(count: Int, mayOpenAfter: (opened: Int) -> Boolean): Int {
        while (held.size < count && mayOpenAfter(held.size)) {
            held += openOne(seed = held.size)
        }
        return held.size
    }

    override fun close() {
        held.forEach { it.close() }
        held.clear()
    }
}

/** What a server holds for a session: a [LiveScreen] being collected, its scene and the last screen it read. */
internal fun liveSessions(screen: @Composable (seed: Int) -> Unit): MemorySessions = object : OneByOneSessions() {
    private val frameTimesRead = AtomicLong()

    override val framesRendered: Long get() = frameTimesRead.get()

    override fun openOne(seed: Int): AutoCloseable {
        val scope = CoroutineScope(context = Dispatchers.Default + SupervisorJob())
        val firstFrame = CompletableDeferred<ScreenFrame>()
        val liveScreen = LiveScreen(
            content = { screen(seed) },
            setup = memoryScreenSetup,
            scaledTextScene = liveScaledTextScene(),
            frameTime = {
                frameTimesRead.incrementAndGet()
                System.nanoTime()
            },
        )
        scope.launch {
            liveScreen.frames()
                .catch { error -> firstFrame.completeExceptionally(error) }
                .collect { frame -> firstFrame.complete(frame) }
        }
        val frame = runBlocking { firstFrame.await() }
        check(frame is ScreenFrame.Sent) { "The memory screen was not sent: $frame" }
        return AutoCloseable { runBlocking { scope.coroutineContext.job.cancelAndJoin() } }
    }
}

/** One scene host per screen, laid out and not read. */
internal fun hostSessions(screen: @Composable (seed: Int) -> Unit): MemorySessions = object : OneByOneSessions() {
    override fun openOne(seed: Int): AutoCloseable {
        val host = openRenderedHost { screen(seed) }
        return AutoCloseable { withSceneLock { host.close() } }
    }
}

/** One scene host per screen, read once as a live session reads it, with what was read kept. */
internal fun readHostSessions(): MemorySessions = object : OneByOneSessions() {
    override fun openOne(seed: Int): AutoCloseable {
        val host = openRenderedHost { MemoryScreen(seed = seed) }
        val read = withSceneLock { readMemoryScreen(roots = host.semanticsRoots()) }
        return KeptScreen(read = read) { withSceneLock { host.close() } }
    }
}

/**
 * The scene the producer used before the layout-only host: Compose UI's `ImageComposeScene` with its raster surface,
 * drawn and read once, with what was read kept.
 */
internal fun drawnSessions(): MemorySessions = object : OneByOneSessions() {
    override fun openOne(seed: Int): AutoCloseable = withSceneLock {
        val scene = ImageComposeScene(
            width = memoryScreenSetup.widthPx,
            height = memoryScreenSetup.heightPx,
            density = memoryScreenSetup.density,
        )
        scene.setContent {
            CompositionLocalProvider(LocalScaledTextScene provides liveScaledTextScene()()) {
                MemoryScreen(seed = seed)
            }
        }
        scene.render().close()
        val roots = scene.semanticsOwners.map { owner -> owner.unmergedRootSemanticsNode }
        KeptScreen(read = readMemoryScreen(roots = roots)) { withSceneLock { scene.close() } }
    }
}

/** Every screen in one scene, in columns: what the screens cost without a scene each. */
internal fun oneSceneSessions(): MemorySessions = object : MemorySessions {
    private var host: SceneHost? = null

    override fun open(count: Int, mayOpenAfter: (opened: Int) -> Boolean): Int {
        host = openRenderedHost { Grid(screens = count) }
        return count
    }

    override fun close() {
        withSceneLock { host?.close() }
        host = null
    }
}

private class KeptScreen(var read: ScreenRead?, private val closeScene: () -> Unit) : AutoCloseable {
    override fun close() {
        closeScene()
        read = null
    }
}

private fun readMemoryScreen(roots: List<SemanticsNode>): ScreenRead {
    val read = readScreen(
        roots = roots,
        density = memoryScreenSetup.density,
        fontScaleCurve = liveScaledTextScene()().curve,
    )
    check(read is ScreenRead.Document) { "The memory screen cannot be sent: $read" }
    return read
}

private fun openRenderedHost(content: @Composable () -> Unit): SceneHost = withSceneLock {
    openSceneHost(setup = memoryScreenSetup, coroutineContext = Dispatchers.Unconfined).also { host ->
        host.setContent {
            CompositionLocalProvider(LocalScaledTextScene provides liveScaledTextScene()()) { content() }
        }
        host.renderFrame(nanoTime = System.nanoTime())
    }
}

@Composable
private fun Grid(screens: Int) {
    Row(modifier = Modifier.wrapContentWidth(unbounded = true).wrapContentHeight(unbounded = true)) {
        (0 until screens).chunked(SCREENS_PER_COLUMN).forEach { column ->
            Column(modifier = Modifier.width(SCREEN_WIDTH_DP.dp)) {
                column.forEach { seed -> MemoryScreen(seed = seed, modifier = Modifier.height(SCREEN_HEIGHT_DP.dp)) }
            }
        }
    }
}
