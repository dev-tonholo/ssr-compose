package dev.tonholo.ssrcompose.producer.internal.scene

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.unit.Density
import dev.tonholo.ssrcompose.core.handshake.FontScaleCurve
import dev.tonholo.ssrcompose.producer.internal.compat.SceneHost
import dev.tonholo.ssrcompose.producer.internal.compat.openSceneHost
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
 * Every step that touches the scene's nodes holds the scene lock, for the reason [SceneHost] gives.
 */
internal class ScreenScene(
    setup: SceneSetup,
    coroutineContext: CoroutineContext,
    private val frameTime: FrameTimeSource = FrameTimeSource.Monotonic,
    private val readScaledTextScene: () -> ScaledTextScene,
) : AutoCloseable {
    private val host = withSceneLock { openSceneHost(setup = setup, coroutineContext = coroutineContext) }

    val density: Density = setup.density

    val fontScaleCurve: FontScaleCurve get() = readScaledTextScene().curve

    /** Composes [content] inside the producer's root `Box`, and throws what its first composition throws. */
    fun setContent(content: @Composable () -> Unit) {
        withSceneLock {
            host.setContent {
                CompositionLocalProvider(LocalScaledTextScene provides readScaledTextScene()) {
                    Box(modifier = Modifier.fillMaxSize()) { content() }
                }
            }
        }
    }

    /** Gives [block] the [SceneHost.semanticsRoots], and holds the scene lock until it returns. */
    fun <T> read(block: (roots: List<SemanticsNode>) -> T): T = withSceneLock { block(host.semanticsRoots()) }

    fun hasInvalidations(): Boolean = host.hasPendingWork()

    /** Recomposes, measures and places what changed. */
    fun render() {
        withSceneLock { host.renderFrame(nanoTime = frameTime.nanoTime()) }
    }

    override fun close() {
        withSceneLock { host.close() }
    }

    private companion object {
        init {
            isDebugInspectorInfoEnabled = true
        }
    }
}
