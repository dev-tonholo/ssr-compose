package dev.tonholo.ssrcompose.producer.internal.compat

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsNode
import dev.tonholo.ssrcompose.producer.internal.scene.SceneSetup
import kotlin.coroutines.CoroutineContext

/**
 * The Compose scene of one screen, and the only way the producer reaches the engine that runs it.
 *
 * A host is not safe for two threads at once. Compose UI also keeps a process-wide cache of node kinds with no lock of
 * its own, and two scenes building nodes at once on two threads corrupt it. So a caller holds the process's one scene
 * lock while it opens a host and around every call but [hasPendingWork], including while it reads the semantics nodes.
 * Every implementation passes `SceneHostConformanceTest`.
 */
internal interface SceneHost : AutoCloseable {
    /** Composes [content] over the whole screen, and throws what its first composition throws. */
    fun setContent(content: @Composable () -> Unit)

    /**
     * Runs one frame at [nanoTime], which `withFrameNanos` reads, then measures and places what changed. Nothing is
     * drawn.
     */
    fun renderFrame(nanoTime: Long)

    /** The roots of the unmerged semantics trees, one per window: the screen's, and one per popup or dialog. */
    fun semanticsRoots(): List<SemanticsNode>

    /** Whether the next frame has something to do. */
    fun hasPendingWork(): Boolean

    override fun close()
}

/**
 * Opens a host for the screen [setup] describes. Its effects and recompositions run on [coroutineContext].
 */
internal fun openSceneHost(setup: SceneSetup, coroutineContext: CoroutineContext): SceneHost =
    SkikoSceneHost(setup = setup, coroutineContext = coroutineContext)
