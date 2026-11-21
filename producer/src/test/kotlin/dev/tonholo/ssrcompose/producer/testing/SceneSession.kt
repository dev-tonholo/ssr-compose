package dev.tonholo.ssrcompose.producer.testing

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import dev.tonholo.ssrcompose.producer.internal.scene.SceneSetup
import dev.tonholo.ssrcompose.producer.internal.scene.ScreenScene
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers

internal const val DEFAULT_SCENE_WIDTH_PX = 400
internal const val DEFAULT_SCENE_HEIGHT_PX = 800

internal data class SceneSpec(
    val widthPx: Int = DEFAULT_SCENE_WIDTH_PX,
    val heightPx: Int = DEFAULT_SCENE_HEIGHT_PX,
    val density: Density = Density(density = 1f, fontScale = 1f),
    val layoutDirection: LayoutDirection = LayoutDirection.Ltr,
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
).apply { setContent(content = content) }

internal fun <T> withRenderedScene(
    spec: SceneSpec = SceneSpec(),
    content: @Composable () -> Unit,
    read: (ScreenScene) -> T,
): T = openTestScene(spec = spec, content = content).use { scene ->
    scene.render()
    read(scene)
}
