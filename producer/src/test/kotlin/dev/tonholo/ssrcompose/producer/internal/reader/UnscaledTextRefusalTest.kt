package dev.tonholo.ssrcompose.producer.internal.reader

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tonholo.ssrcompose.producer.ScaledBasicText
import dev.tonholo.ssrcompose.producer.internal.text.ScaledTextMark
import dev.tonholo.ssrcompose.producer.internal.text.scaledTextMark
import dev.tonholo.ssrcompose.producer.testing.LARGE_SIZE_SP
import dev.tonholo.ssrcompose.producer.testing.SAMPLE_TEXT
import dev.tonholo.ssrcompose.producer.testing.SMALL_SIZE_SP
import dev.tonholo.ssrcompose.producer.testing.SceneSpec
import dev.tonholo.ssrcompose.producer.testing.emulatorSpec
import dev.tonholo.ssrcompose.producer.testing.readSceneOf
import dev.tonholo.ssrcompose.producer.testing.robotoStyle
import kotlin.test.Test
import kotlin.test.assertEquals

private const val ICON_ID = "icon"
private const val ICON_SIZE_SP = 12
private const val AUTO_SIZE_MIN_SP = 8
private const val BOX_WIDTH_DP = 120
private const val BOX_HEIGHT_DP = 40
private const val OVERRIDE_DENSITY = 3f
private const val OVERRIDE_FONT_SCALE = 1.04f
private val UNSCALED = ScreenRead.NotSendable(reason = NotSendableReason.UnscaledText)

class UnscaledTextRefusalTest {
    private val fontScaleOne = SceneSpec()
    private val fontScaleOnePointThree = emulatorSpec()

    @Test
    fun `given a raw BasicText, when read at font scale 1 and 1_3, then it is not sendable as an unscaled text`() {
        // Arrange
        val specs = listOf(fontScaleOne, fontScaleOnePointThree)

        // Act
        val actual = specs.map { spec ->
            readSceneOf(spec = spec) { BasicText(text = SAMPLE_TEXT, style = robotoStyle(sizeSp = SMALL_SIZE_SP)) }
        }

        // Assert
        assertEquals(expected = listOf(UNSCALED, UNSCALED), actual = actual)
    }

    @Test
    fun `given a function reference to BasicText, when read, then it is not sendable as an unscaled text`() {
        // Arrange
        val reference: @Composable (String) -> Unit = ::BasicText

        // Act
        val actual = readSceneOf(spec = fontScaleOnePointThree) { reference(SAMPLE_TEXT) }

        // Assert
        assertEquals(expected = UNSCALED, actual = actual)
    }

    @Test
    fun `given autoSize in ScaledBasicText, when read at font scale 1 and 1_3, then it is not sendable as unscaled`() {
        // Arrange
        val specs = listOf(fontScaleOne, fontScaleOnePointThree)
        val autoSize = TextAutoSize.StepBased(minFontSize = AUTO_SIZE_MIN_SP.sp, maxFontSize = LARGE_SIZE_SP.sp)

        // Act
        val actual = specs.map { spec ->
            readSceneOf(spec = spec) {
                ScaledBasicText(
                    text = SAMPLE_TEXT,
                    modifier = Modifier.size(width = BOX_WIDTH_DP.dp, height = BOX_HEIGHT_DP.dp),
                    style = robotoStyle(sizeSp = LARGE_SIZE_SP),
                    autoSize = autoSize,
                )
            }
        }

        // Assert
        assertEquals(expected = listOf(UNSCALED, UNSCALED), actual = actual)
    }

    @Test
    fun `given a marked text laid out off the curve, when read, then it is not sendable as unscaled`() {
        // Arrange
        val mark = ScaledTextMark(sizeSp = LARGE_SIZE_SP.toFloat(), lineHeightSp = null)

        // Act
        val actual = readSceneOf(spec = fontScaleOnePointThree) {
            BasicText(
                text = SAMPLE_TEXT,
                modifier = Modifier.semantics { this[scaledTextMark] = mark },
                style = robotoStyle(sizeSp = LARGE_SIZE_SP),
            )
        }

        // Assert
        assertEquals(expected = UNSCALED, actual = actual)
    }

    @Test
    fun `given ScaledBasicText under a density override, when read, then it is not sendable as unscaled`() {
        // Arrange
        val override = Density(density = OVERRIDE_DENSITY, fontScale = OVERRIDE_FONT_SCALE)

        // Act
        val actual = readSceneOf(spec = fontScaleOnePointThree) {
            CompositionLocalProvider(LocalDensity provides override) {
                ScaledBasicText(text = SAMPLE_TEXT, style = robotoStyle(sizeSp = SMALL_SIZE_SP))
            }
        }

        // Assert
        assertEquals(expected = UNSCALED, actual = actual)
    }

    @Test
    fun `given a text with inline content, when read, then it is not sendable for its several styles`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.SeveralStyles)
        val text = buildAnnotatedString {
            append(SAMPLE_TEXT)
            appendInlineContent(id = ICON_ID)
        }
        val placeholder = Placeholder(
            width = ICON_SIZE_SP.sp,
            height = ICON_SIZE_SP.sp,
            placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
        )

        // Act
        val actual = readSceneOf(spec = fontScaleOnePointThree) {
            ScaledBasicText(
                text = text,
                style = robotoStyle(sizeSp = SMALL_SIZE_SP),
                inlineContent = mapOf(ICON_ID to InlineTextContent(placeholder = placeholder, children = {})),
            )
        }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }
}
