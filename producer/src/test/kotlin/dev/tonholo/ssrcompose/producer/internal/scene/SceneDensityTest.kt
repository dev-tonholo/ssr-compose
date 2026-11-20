package dev.tonholo.ssrcompose.producer.internal.scene

import androidx.compose.ui.unit.Density
import dev.tonholo.ssrcompose.producer.testing.TEST_DENSITY
import dev.tonholo.ssrcompose.producer.testing.TEST_FONT_SCALE
import dev.tonholo.ssrcompose.producer.testing.testDevice
import kotlin.test.Test
import kotlin.test.assertEquals

class SceneDensityTest {
    @Test
    fun `given a device context, when the scene density is built, then it has the device density and font scale`() {
        // Arrange
        val device = testDevice(density = TEST_DENSITY, fontScale = TEST_FONT_SCALE)
        val expected = Density(density = TEST_DENSITY, fontScale = TEST_FONT_SCALE)

        // Act
        val actual = sceneDensity(device = device)

        // Assert
        assertEquals(expected = expected.density, actual = actual.density)
        assertEquals(expected = expected.fontScale, actual = actual.fontScale)
    }
}
