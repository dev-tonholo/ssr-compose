package dev.tonholo.ssrcompose.producer.internal.scene

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Density
import dev.tonholo.ssrcompose.producer.internal.text.LocalScaledTextScene
import dev.tonholo.ssrcompose.producer.internal.text.ScaledTextScene
import dev.tonholo.ssrcompose.producer.testing.BAND_CURVE
import dev.tonholo.ssrcompose.producer.testing.EMULATOR_CURVE
import dev.tonholo.ssrcompose.producer.testing.EMULATOR_FONT_SCALE
import dev.tonholo.ssrcompose.producer.testing.SceneSpec
import dev.tonholo.ssrcompose.producer.testing.TEST_DENSITY
import dev.tonholo.ssrcompose.producer.testing.openTestScene
import dev.tonholo.ssrcompose.producer.testing.withBareScene
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FontScaleCurveProvisionTest {
    @Test
    fun `given a scene with a curve and a font scale, when content reads LocalScaledTextScene, then it reads both`() {
        // Arrange
        var read: ScaledTextScene? = null
        val density = Density(density = TEST_DENSITY, fontScale = EMULATOR_FONT_SCALE)
        val spec = SceneSpec(density = density, fontScaleCurve = { EMULATOR_CURVE })
        val expected = ScaledTextScene(curve = EMULATOR_CURVE, fontScale = EMULATOR_FONT_SCALE)

        // Act
        openTestScene(spec = spec) { read = LocalScaledTextScene.current }.use { scene -> scene.render() }

        // Assert
        assertEquals(expected = expected, actual = read)
    }

    @Test
    fun `given no producer screen, when a composition reads LocalScaledTextScene, then it reads null`() {
        // Arrange
        var read: ScaledTextScene? = ScaledTextScene(curve = EMULATOR_CURVE, fontScale = EMULATOR_FONT_SCALE)

        // Act
        withBareScene(content = { read = LocalScaledTextScene.current }, read = {})

        // Assert
        assertNull(actual = read)
    }

    @Test
    fun `given a curve that changes, when the scene renders again, then its content reads the new curve`() {
        // Arrange
        val device = mutableStateOf(value = EMULATOR_CURVE)
        var read: ScaledTextScene? = null
        val spec = SceneSpec(fontScaleCurve = { device.value })

        // Act
        val actual = openTestScene(spec = spec) { read = LocalScaledTextScene.current }.use { scene ->
            scene.render()
            device.value = BAND_CURVE
            scene.render()
            read?.curve
        }

        // Assert
        assertEquals(expected = BAND_CURVE, actual = actual)
    }
}
