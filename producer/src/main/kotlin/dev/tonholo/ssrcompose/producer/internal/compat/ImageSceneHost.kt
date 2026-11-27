package dev.tonholo.ssrcompose.producer.internal.compat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.SemanticsNode
import dev.tonholo.ssrcompose.producer.internal.scene.SceneSetup
import kotlin.coroutines.CoroutineContext

/**
 * A [SceneHost] over Compose UI's public [ImageComposeScene], which also draws every frame into a raster surface of the
 * screen's size.
 */
internal class ImageSceneHost(
    private val setup: SceneSetup,
    coroutineContext: CoroutineContext,
) : SceneHost {
    private val scene = ImageComposeScene(
        width = setup.widthPx,
        height = setup.heightPx,
        density = setup.density,
        coroutineContext = coroutineContext,
    )

    override fun setContent(content: @Composable () -> Unit) {
        scene.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides setup.layoutDirection, content = content)
        }
    }

    override fun renderFrame(nanoTime: Long) {
        scene.render(nanoTime = nanoTime).close()
    }

    override fun semanticsRoots(): List<SemanticsNode> =
        scene.semanticsOwners.map { owner -> owner.unmergedRootSemanticsNode }

    override fun hasPendingWork(): Boolean = scene.hasInvalidations()

    override fun close() {
        scene.close()
    }
}
