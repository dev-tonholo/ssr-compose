package dev.tonholo.ssrcompose.producer.internal.scene

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import dev.tonholo.ssrcompose.producer.testing.DEFAULT_SCENE_HEIGHT_PX
import dev.tonholo.ssrcompose.producer.testing.DEFAULT_SCENE_WIDTH_PX
import dev.tonholo.ssrcompose.producer.testing.liveScaledTextScene
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers

private const val FIRST_FRAME_NANOS = 1_000_000_000L
private const val FRAME_STEP_NANOS = 16_000_000L
private const val FRAMES = 3

class ScreenSceneFrameTimeTest {
    @Test
    fun `given a clock that advances, when the scene renders several frames, then each frame gets the clock's time`() {
        // Arrange
        var now = FIRST_FRAME_NANOS - FRAME_STEP_NANOS
        val clock = FrameTimeSource {
            now += FRAME_STEP_NANOS
            now
        }
        val seen = mutableListOf<Long>()
        val scene = ScreenScene(
            setup = SceneSetup(
                widthPx = DEFAULT_SCENE_WIDTH_PX,
                heightPx = DEFAULT_SCENE_HEIGHT_PX,
                density = Density(density = 1f),
                layoutDirection = LayoutDirection.Ltr,
            ),
            frameTime = clock,
            coroutineContext = Dispatchers.Unconfined,
            readScaledTextScene = liveScaledTextScene(),
        )
        scene.setContent { LaunchedEffect(Unit) { repeat(FRAMES) { withFrameNanos { seen += it } } } }

        // Act
        repeat(FRAMES + 1) { scene.render() }
        scene.close()

        // Assert
        val expected = List(FRAMES) { index -> FIRST_FRAME_NANOS + index * FRAME_STEP_NANOS }
        assertEquals(expected = expected, actual = seen)
    }
}
