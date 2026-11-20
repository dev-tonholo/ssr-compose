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
 */
internal class ScreenScene(
    setup: SceneSetup,
    coroutineContext: CoroutineContext,
    content: @Composable () -> Unit,
) : AutoCloseable {
    private val scene = ImageComposeScene(
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

    val density: Density = setup.density

    /** The root of the unmerged semantics tree, the node of the producer's root `Box`. */
    val root: SemanticsNode
        get() = scene.semanticsOwners.single().unmergedRootSemanticsNode

    fun hasInvalidations(): Boolean = scene.hasInvalidations()

    /** Recomposes, measures and places what changed. */
    fun render() {
        scene.render().close()
    }

    override fun close() {
        scene.close()
    }

    private companion object {
        init {
            isDebugInspectorInfoEnabled = true
        }
    }
}
