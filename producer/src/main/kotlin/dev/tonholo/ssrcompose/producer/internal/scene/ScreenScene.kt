package dev.tonholo.ssrcompose.producer.internal.scene

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import dev.tonholo.ssrcompose.core.handshake.FontScaleCurve
import dev.tonholo.ssrcompose.producer.internal.text.LocalScaledTextScene
import dev.tonholo.ssrcompose.producer.internal.text.ScaledTextScene
import kotlin.coroutines.CoroutineContext

/**
 * One screen composed, measured and placed by Compose UI, inside the root `Box` the producer wraps around every
 * screen. Create it, set its content, render, read and close it on one thread only, the producer's scene thread, which
 * [coroutineContext] must also run on. It shows nothing until [setContent].
 *
 * [readScaledTextScene] gives the device's curve and font scale together. Composition reads it, so values held in
 * state recompose the texts when they change.
 *
 * Every step that touches the scene's nodes holds one lock for the whole process: Compose UI keeps a process-wide
 * cache of node kinds with no lock of its own, and two scenes building nodes at once on two threads corrupt it.
 */
internal class ScreenScene(
    setup: SceneSetup,
    coroutineContext: CoroutineContext,
    private val frameTime: FrameTimeSource = FrameTimeSource.Monotonic,
    private val readScaledTextScene: () -> ScaledTextScene,
) : AutoCloseable {
    private val layoutDirection: LayoutDirection = setup.layoutDirection

    private val scene = withSceneLock {
        ImageComposeScene(
            width = setup.widthPx,
            height = setup.heightPx,
            density = setup.density,
            coroutineContext = coroutineContext,
        )
    }

    val density: Density = setup.density

    val fontScaleCurve: FontScaleCurve get() = readScaledTextScene().curve

    /** Composes [content] inside the producer's root `Box`, and throws what its first composition throws. */
    fun setContent(content: @Composable () -> Unit) {
        withSceneLock {
            scene.setContent {
                CompositionLocalProvider(
                    LocalLayoutDirection provides layoutDirection,
                    LocalScaledTextScene provides readScaledTextScene(),
                ) {
                    Box(modifier = Modifier.fillMaxSize()) { content() }
                }
            }
        }
    }

    /** Reads the roots of the unmerged semantics trees, one per window: the screen's, and one per popup or dialog. */
    fun <T> read(block: (roots: List<SemanticsNode>) -> T): T =
        withSceneLock { block(scene.semanticsOwners.map { owner -> owner.unmergedRootSemanticsNode }) }

    fun hasInvalidations(): Boolean = scene.hasInvalidations()

    /** Recomposes, measures and places what changed. */
    fun render() {
        withSceneLock { scene.render(nanoTime = frameTime.nanoTime()).close() }
    }

    override fun close() {
        withSceneLock { scene.close() }
    }

    private companion object {
        init {
            isDebugInspectorInfoEnabled = true
        }
    }
}
