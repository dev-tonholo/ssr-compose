package dev.tonholo.ssrcompose.producer.testing

import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import dev.tonholo.ssrcompose.core.handshake.FontScaleCurve
import dev.tonholo.ssrcompose.producer.internal.scene.SceneSetup
import dev.tonholo.ssrcompose.producer.internal.scene.ScreenScene
import dev.tonholo.ssrcompose.producer.internal.scene.withSceneLock
import dev.tonholo.ssrcompose.producer.internal.text.ScaledTextScene
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers

internal const val DEFAULT_SCENE_WIDTH_PX = 400
internal const val DEFAULT_SCENE_HEIGHT_PX = 800

internal data class SceneSpec(
    val widthPx: Int = DEFAULT_SCENE_WIDTH_PX,
    val heightPx: Int = DEFAULT_SCENE_HEIGHT_PX,
    val density: Density = Density(density = 1f, fontScale = 1f),
    val layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    val fontScaleCurve: () -> FontScaleCurve = { linearCurve(scale = density.fontScale) },
    val fontScale: () -> Float = { density.fontScale },
    val dispatcher: CoroutineContext = Dispatchers.Unconfined,
)

internal fun openTestScene(spec: SceneSpec, content: @Composable () -> Unit): ScreenScene = ScreenScene(
    setup = SceneSetup(
        widthPx = spec.widthPx,
        heightPx = spec.heightPx,
        density = spec.density,
        layoutDirection = spec.layoutDirection,
    ),
    coroutineContext = spec.dispatcher,
    readScaledTextScene = { ScaledTextScene(curve = spec.fontScaleCurve(), fontScale = spec.fontScale()) },
).apply { setContent(content = content) }

internal fun <T> withRenderedScene(
    spec: SceneSpec = SceneSpec(),
    content: @Composable () -> Unit,
    read: (ScreenScene) -> T,
): T = openTestScene(spec = spec, content = content).use { scene ->
    scene.render()
    read(scene)
}

/**
 * Composes [content] in a scene with no producer screen around it, as a preview or a server developer's own Compose
 * test does, and gives [read] the roots of its semantics trees.
 */
internal fun <T> withBareScene(
    spec: SceneSpec = SceneSpec(),
    content: @Composable () -> Unit,
    read: (roots: List<SemanticsNode>) -> T,
): T = withSceneLock {
    val scene = ImageComposeScene(
        width = spec.widthPx,
        height = spec.heightPx,
        density = spec.density,
        coroutineContext = spec.dispatcher,
    )
    try {
        scene.setContent(content = content)
        scene.render().close()
        read(scene.semanticsOwners.map { owner -> owner.unmergedRootSemanticsNode })
    } finally {
        scene.close()
    }
}
