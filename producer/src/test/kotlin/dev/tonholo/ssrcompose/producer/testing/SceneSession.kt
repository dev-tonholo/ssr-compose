package dev.tonholo.ssrcompose.producer.testing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
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

internal class SceneSession(spec: SceneSpec, content: @Composable () -> Unit) : AutoCloseable {
    init {
        isDebugInspectorInfoEnabled = true
    }

    val scene = ImageComposeScene(
        width = spec.widthPx,
        height = spec.heightPx,
        density = spec.density,
        coroutineContext = spec.dispatcher,
        content = { CompositionLocalProvider(LocalLayoutDirection provides spec.layoutDirection) { content() } },
    )

    fun render() {
        scene.render()
    }

    override fun close() {
        scene.close()
    }
}

internal fun <T> withRenderedScene(
    spec: SceneSpec = SceneSpec(),
    content: @Composable () -> Unit,
    read: (ImageComposeScene) -> T,
): T = SceneSession(spec, content).use { session ->
    session.render()
    read(session.scene)
}
