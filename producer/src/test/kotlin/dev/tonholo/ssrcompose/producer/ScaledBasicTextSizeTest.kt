package dev.tonholo.ssrcompose.producer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.tonholo.ssrcompose.core.handshake.FontScaleCurve
import dev.tonholo.ssrcompose.producer.testing.EMULATOR_CURVE
import dev.tonholo.ssrcompose.producer.testing.EMULATOR_FONT_SCALE
import dev.tonholo.ssrcompose.producer.testing.LARGE_SIZE_SP
import dev.tonholo.ssrcompose.producer.testing.SAMPLE_TEXT
import dev.tonholo.ssrcompose.producer.testing.SMALL_SIZE_SP
import dev.tonholo.ssrcompose.producer.testing.assertHeights
import dev.tonholo.ssrcompose.producer.testing.bareHeightOf
import dev.tonholo.ssrcompose.producer.testing.documentOf
import dev.tonholo.ssrcompose.producer.testing.emulatorSpec
import dev.tonholo.ssrcompose.producer.testing.firstText
import dev.tonholo.ssrcompose.producer.testing.heightOf
import dev.tonholo.ssrcompose.producer.testing.linearCurve
import dev.tonholo.ssrcompose.producer.testing.robotoStyle
import dev.tonholo.ssrcompose.producer.testing.withRenderedScene
import kotlin.test.Test
import kotlin.test.assertEquals

private const val CURVE_HEIGHT_16_SP = 59f
private const val CURVE_HEIGHT_24_SP = 77f
private const val STRAIGHT_HEIGHT_16_SP = 61f
private const val STRAIGHT_HEIGHT_24_SP = 91f

/**
 * Heights in pixels are the probes of ADR 0023: Skia's own metrics for the bundled Roboto at density 2.5, 59 and 77 px
 * for 16 and 24 sp on the emulator curve, and 61 and 91 px on a straight font scale of 1.3.
 */
class ScaledBasicTextSizeTest {
    private val sizes = listOf(SMALL_SIZE_SP, LARGE_SIZE_SP)

    @Test
    fun `given 16 and 24 sp text on the emulator curve, when laid out, then it is 59 and 77 px high`() {
        // Arrange
        val expected = listOf(CURVE_HEIGHT_16_SP, CURVE_HEIGHT_24_SP)

        // Act
        val actual = sizes.map { size -> heightOf(spec = emulatorSpec(), sizeSp = size) }

        // Assert
        assertHeights(expected = expected, actual = actual)
    }

    @Test
    fun `given 16 and 24 sp text, when read, then the document keeps the size in sp`() {
        // Arrange
        val expected = sizes.map { size -> size.toFloat() }

        // Act
        val actual = sizes.map { size ->
            documentOf(spec = emulatorSpec()) {
                ScaledBasicText(text = SAMPLE_TEXT, style = robotoStyle(sizeSp = size))
            }.root.firstText().style.size
        }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given no producer screen, when laid out, then it is as high as BasicText on a straight font scale`() {
        // Arrange
        val expected = listOf(STRAIGHT_HEIGHT_16_SP, STRAIGHT_HEIGHT_24_SP)

        // Act
        val actual = sizes.map { size -> bareHeightOf(spec = emulatorSpec(), sizeSp = size) }

        // Assert
        assertHeights(expected = expected, actual = actual)
    }

    @Test
    fun `given a curve that changes, when the scene renders again, then the text takes the new size`() {
        // Arrange
        var curve: FontScaleCurve by mutableStateOf(value = EMULATOR_CURVE)
        var height = 0
        val expected = listOf(CURVE_HEIGHT_24_SP, STRAIGHT_HEIGHT_24_SP)

        // Act
        val actual = withRenderedScene(
            spec = emulatorSpec().copy(fontScaleCurve = { curve }),
            content = {
                ScaledBasicText(
                    text = SAMPLE_TEXT,
                    style = robotoStyle(sizeSp = LARGE_SIZE_SP),
                    onTextLayout = { height = it.size.height },
                )
            },
            read = { scene ->
                val first = height
                curve = linearCurve(scale = EMULATOR_FONT_SCALE)
                scene.render()
                listOf(first, height)
            },
        )

        // Assert
        assertHeights(expected = expected, actual = actual)
    }
}
