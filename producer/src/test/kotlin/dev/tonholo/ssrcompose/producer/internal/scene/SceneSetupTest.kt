package dev.tonholo.ssrcompose.producer.internal.scene

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import dev.tonholo.ssrcompose.core.document.Viewport
import dev.tonholo.ssrcompose.producer.testing.testDevice
import kotlin.test.Test
import kotlin.test.assertEquals
import dev.tonholo.ssrcompose.core.handshake.LayoutDirection as DeviceLayoutDirection

private const val DENSITY = 5f
private const val FONT_SCALE = 1.3f

// R20 example 1's viewport, 411.4 by 843.4 dp, is 2057 by 4217 px at density 5.
private val VIEWPORT = Viewport(width = 411.4f, height = 843.4f)
private const val EXPECTED_WIDTH_PX = 2057
private const val EXPECTED_HEIGHT_PX = 4217

class SceneSetupTest {
    @Test
    fun `given a viewport and an Rtl device, when the scene is set up, then it has their pixels and direction`() {
        // Arrange
        val device = testDevice(density = DENSITY, fontScale = FONT_SCALE, layoutDirection = DeviceLayoutDirection.Rtl)
        val expected = SceneSetup(
            widthPx = EXPECTED_WIDTH_PX,
            heightPx = EXPECTED_HEIGHT_PX,
            density = Density(density = DENSITY, fontScale = FONT_SCALE),
            layoutDirection = LayoutDirection.Rtl,
        )

        // Act
        val actual = sceneSetup(viewport = VIEWPORT, device = device)

        // Assert
        assertEquals(expected = expected, actual = actual)
    }
}
