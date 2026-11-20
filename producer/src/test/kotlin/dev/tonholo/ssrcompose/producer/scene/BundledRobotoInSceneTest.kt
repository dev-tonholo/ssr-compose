package dev.tonholo.ssrcompose.producer.scene

import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import dev.tonholo.ssrcompose.producer.internal.font.RobotoFace
import dev.tonholo.ssrcompose.producer.internal.font.bundledRobotoFamily
import dev.tonholo.ssrcompose.producer.testing.withRenderedScene
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.skia.Data
import org.jetbrains.skia.Font
import org.jetbrains.skia.FontHinting
import org.jetbrains.skia.FontMgr

private const val SAMPLE_TEXT = "Served by the producer, measured with Roboto"
private const val SAMPLE_FONT_SIZE = 16
private const val ROUNDING_TOLERANCE_PX = 1f

/**
 * ADR 0020: default text uses the bundled Roboto. Compose offers no public way to remap `FontFamily.Default`, so a
 * server text names the bundled family and the scene must resolve it to the bundled files.
 */
class BundledRobotoInSceneTest {
    @Test
    fun `given the bundled Roboto family, when text is measured, then it measures like the bundled file`() {
        // Arrange
        val typeface = checkNotNull(FontMgr.default.makeFromData(Data.makeFromBytes(RobotoFace.Regular.readBytes())))
        val unhintedFont = Font(typeface, SAMPLE_FONT_SIZE.toFloat()).apply {
            isSubpixel = true
            hinting = FontHinting.NONE
        }
        val expectedWidth = unhintedFont.measureTextWidth(SAMPLE_TEXT)

        // Act
        val actualWidth = measuredWidth(family = bundledRobotoFamily()).toFloat()

        // Assert
        assertEquals(expected = expectedWidth, actual = actualWidth, absoluteTolerance = ROUNDING_TOLERANCE_PX)
    }

    private fun measuredWidth(family: FontFamily): Int {
        var width = 0
        withRenderedScene(
            content = {
                BasicText(
                    text = SAMPLE_TEXT,
                    style = TextStyle(fontSize = SAMPLE_FONT_SIZE.sp, fontFamily = family),
                    onTextLayout = { width = it.size.width },
                )
            },
            read = {},
        )
        return width
    }
}
