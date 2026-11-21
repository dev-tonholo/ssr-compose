package dev.tonholo.ssrcompose.producer

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import dev.tonholo.ssrcompose.producer.testing.BAND_CURVE
import dev.tonholo.ssrcompose.producer.testing.BAND_FONT_SCALE
import dev.tonholo.ssrcompose.producer.testing.EMULATOR_CURVE
import dev.tonholo.ssrcompose.producer.testing.EMULATOR_FONT_SCALE
import dev.tonholo.ssrcompose.producer.testing.LINE_HEIGHT_SP
import dev.tonholo.ssrcompose.producer.testing.SMALL_SIZE_SP
import dev.tonholo.ssrcompose.producer.testing.TEST_DENSITY
import dev.tonholo.ssrcompose.producer.testing.TWO_LINES_TEXT
import dev.tonholo.ssrcompose.producer.testing.WIDE_SCENE_PX
import dev.tonholo.ssrcompose.producer.testing.assertHeights
import dev.tonholo.ssrcompose.producer.testing.bandSpec
import dev.tonholo.ssrcompose.producer.testing.emulatorSpec
import dev.tonholo.ssrcompose.producer.testing.heightOf
import dev.tonholo.ssrcompose.producer.testing.heightOfBasicText
import dev.tonholo.ssrcompose.producer.testing.openTestScene
import dev.tonholo.ssrcompose.producer.testing.robotoStyle
import dev.tonholo.ssrcompose.producer.testing.withRenderedScene
import kotlin.test.Test

private const val TWO_LINES_HEIGHT = 135f
private const val WIDE_SCENE_PX = 1000
private const val BAND_FONT_SCALE = 1.04f

/**
 * 135 px is the probe G of ADR 0023: two lines of 16 sp text with a 24 sp line height at density 2.5 and font scale
 * 1.3, which is what Android draws when it makes a line height proportional to the converted text size.
 */
class ScaledBasicTextLineHeightTest {
    private val style = robotoStyle(sizeSp = SMALL_SIZE_SP).copy(lineHeight = LINE_HEIGHT_SP.sp)

    @Test
    fun `given a 24 sp line height on 16 sp text, when laid out in two lines at font scale 1_3, then it is 135 px`() {
        // Arrange
        val expected = listOf(TWO_LINES_HEIGHT)

        // Act
        val actual = heightOf(spec = emulatorSpec(), text = TWO_LINES_TEXT, style = style)

        // Assert
        assertHeights(expected = expected, actual = listOf(actual))
    }

    @Test
    fun `given a 24 sp line height at font scale 1_04, when laid out, then each line is curve of 24 dp high`() {
        // Arrange
        val expected = listOf(heightOfBasicText(curve = BAND_CURVE).toFloat())

        // Act
        val actual = heightOf(spec = bandSpec(), text = TWO_LINES_TEXT, style = style)

        // Assert
        assertHeights(expected = expected, actual = listOf(actual))
    }

    @Test
    fun `given a font scale override of 1_04 on a scene at 1_3, when laid out, then the band rule is not applied`() {
        // Arrange
        val override = Density(density = TEST_DENSITY, fontScale = BAND_FONT_SCALE)
        val expected = listOf(TWO_LINES_HEIGHT)
        var laidOut = 0

        // Act
        withRenderedScene(
            spec = emulatorSpec(),
            content = {
                CompositionLocalProvider(LocalDensity provides override) {
                    ScaledBasicText(
                        text = TWO_LINES_TEXT,
                        style = style,
                        onTextLayout = { laidOut = it.size.height },
                    )
                }
            },
            read = {},
        )

        // Assert
        assertHeights(expected = expected, actual = listOf(laidOut))
    }

    @Test
    fun `given a font scale that moves into the band, when it renders again, then each line is curve of 24 dp high`() {
        // Arrange
        val curve = mutableStateOf(value = EMULATOR_CURVE)
        val fontScale = mutableStateOf(value = EMULATOR_FONT_SCALE)
        val spec = emulatorSpec().copy(fontScaleCurve = { curve.value }, fontScale = { fontScale.value })
        val expected = listOf(heightOfBasicText(curve = BAND_CURVE).toFloat())
        var laidOut = 0

        // Act
        openTestScene(spec = spec) {
            ScaledBasicText(text = TWO_LINES_TEXT, style = style, onTextLayout = { laidOut = it.size.height })
        }.use { scene ->
            scene.render()
            curve.value = BAND_CURVE
            fontScale.value = BAND_FONT_SCALE
            scene.render()
        }

        // Assert
        assertHeights(expected = expected, actual = listOf(laidOut))
    }
}
