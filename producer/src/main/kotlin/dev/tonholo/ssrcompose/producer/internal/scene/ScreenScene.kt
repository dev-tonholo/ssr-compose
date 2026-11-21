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
import kotlin.coroutines.CoroutineContext

/**
 * One screen composed, measured and placed by Compose UI, inside the root `Box` the producer wraps around every
 * screen. Create, render and close it on the session's thread only, which [coroutineContext] must also run on.
 *
 * Every step that touches the scene's nodes holds one lock for the whole process: Compose UI keeps a process-wide
 * cache of node kinds with no lock of its own, and two scenes building nodes at once on two threads corrupt it.
 */
internal class ScreenScene(
    setup: SceneSetup,
    coroutineContext: CoroutineContext,
    content: @Composable () -> Unit,
) : AutoCloseable {
    private val scene = withSceneLock {
        ImageComposeScene(
            width = setup.widthPx,
            height = setup.heightPx,
            density = setup.density,
            coroutineContext = coroutineContext,
            content = {
                CompositionLocalProvider(LocalLayoutDirection provides setup.layoutDirection) {
                    Box(modifier = Modifier.fillMaxSize()) { content() }
                }
            },
        )
    }

    val density: Density = setup.density

    /** Reads the roots of the unmerged semantics trees, one per window: the screen's, and one per popup or dialog. */
    fun <T> read(block: (roots: List<SemanticsNode>) -> T): T =
        withSceneLock { block(scene.semanticsOwners.map { owner -> owner.unmergedRootSemanticsNode }) }

    fun hasInvalidations(): Boolean = scene.hasInvalidations()

    /** Recomposes, measures and places what changed. */
    fun render() {
        withSceneLock { scene.render().close() }
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
