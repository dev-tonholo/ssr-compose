package dev.tonholo.ssrcompose.producer.internal.reader

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.tonholo.ssrcompose.core.document.DocumentNode
import dev.tonholo.ssrcompose.producer.ScaledBasicText
import dev.tonholo.ssrcompose.producer.testing.documentOf
import dev.tonholo.ssrcompose.producer.testing.readSceneOf
import dev.tonholo.ssrcompose.producer.testing.robotoStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private const val TEXT = "text"
private const val TEXT_SIZE_SP = 14
private const val DOUBLE_SIZE_EM = 2
private const val LINE_HEIGHT_EM = 1.5f
private const val LINE_HEIGHT_SP = 24
private const val LETTER_SPACING_SP = 1
private const val INDENT_SP = 4
private const val SHADOW_OFFSET_PX = 2f
private const val SHADOW_BLUR_PX = 3f
private const val ONE_LINE = 1
private const val STRETCH_X = 2f
private const val STROKE_WIDTH_PX = 1f
private val UNSUPPORTED_STYLE = ScreenRead.NotSendable(reason = NotSendableReason.UnsupportedTextStyle)
private val GRADIENT = Brush.verticalGradient(colors = listOf(Color.Red, Color.Blue))

class TextRefusalTest {
    @Test
    fun `given a plain text in the bundled Roboto, when read, then it is sent`() {
        // Arrange
        val style = robotoStyle(TEXT_SIZE_SP)

        // Act
        val actual = readSceneOf { ScaledBasicText(text = TEXT, style = style) }

        // Assert
        assertIs<ScreenRead.Document>(value = actual)
    }

    @Test
    fun `given a text sized in em, when read, then it is not sendable for its relative font size`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.RelativeFontSize)

        // Act
        val actual = readSceneOf {
            ScaledBasicText(text = TEXT, style = robotoStyle(TEXT_SIZE_SP).copy(fontSize = DOUBLE_SIZE_EM.em))
        }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a text with a line height in em, when read, then it is not sendable for its relative line height`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.RelativeLineHeight)

        // Act
        val actual = readSceneOf {
            ScaledBasicText(text = TEXT, style = robotoStyle(TEXT_SIZE_SP).copy(lineHeight = LINE_HEIGHT_EM.em))
        }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a text with a line height in sp, when read, then the document has that line height`() {
        // Arrange
        val expected = listOf(LINE_HEIGHT_SP.toFloat())

        // Act
        val actual = documentOf {
            ScaledBasicText(text = TEXT, style = robotoStyle(TEXT_SIZE_SP).copy(lineHeight = LINE_HEIGHT_SP.sp))
        }.root.children.map { (it as DocumentNode.Text).style.lineHeight }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a text field, when read, then it is not sendable as a text field`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.TextField)

        // Act
        val actual = readSceneOf {
            BasicTextField(value = TEXT, onValueChange = {}, textStyle = robotoStyle(TEXT_SIZE_SP))
        }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a text that overflows visibly, when read, then it is not sendable for its style`() {
        // Arrange
        val overflow = TextOverflow.Visible

        // Act
        val actual = readSceneOf {
            ScaledBasicText(text = TEXT, style = robotoStyle(TEXT_SIZE_SP), overflow = overflow)
        }

        // Assert
        assertEquals(expected = UNSUPPORTED_STYLE, actual = actual)
    }

    @Test
    fun `given a text with an ellipsis at its start, when read, then it is not sendable for its style`() {
        // Arrange
        val overflow = TextOverflow.StartEllipsis

        // Act
        val actual = readSceneOf {
            ScaledBasicText(text = TEXT, style = robotoStyle(TEXT_SIZE_SP), overflow = overflow, maxLines = ONE_LINE)
        }

        // Assert
        assertEquals(expected = UNSUPPORTED_STYLE, actual = actual)
    }

    @Test
    fun `given a text with an ellipsis in its middle, when read, then it is not sendable for its style`() {
        // Arrange
        val overflow = TextOverflow.MiddleEllipsis

        // Act
        val actual = readSceneOf {
            ScaledBasicText(text = TEXT, style = robotoStyle(TEXT_SIZE_SP), overflow = overflow, maxLines = ONE_LINE)
        }

        // Assert
        assertEquals(expected = UNSUPPORTED_STYLE, actual = actual)
    }

    @Test
    fun `given a text with no soft wrap, when read, then it is not sendable for its style`() {
        // Arrange
        val softWrap = false

        // Act
        val actual = readSceneOf {
            ScaledBasicText(text = TEXT, style = robotoStyle(TEXT_SIZE_SP), softWrap = softWrap)
        }

        // Assert
        assertEquals(expected = UNSUPPORTED_STYLE, actual = actual)
    }

    @Test
    fun `given a text with letter spacing, when read, then it is not sendable for its style`() {
        // Arrange
        val style = robotoStyle(TEXT_SIZE_SP).copy(letterSpacing = LETTER_SPACING_SP.sp)

        // Act
        val actual = readSceneOf { ScaledBasicText(text = TEXT, style = style) }

        // Assert
        assertEquals(expected = UNSUPPORTED_STYLE, actual = actual)
    }

    @Test
    fun `given an underlined text, when read, then it is not sendable for its style`() {
        // Arrange
        val style = robotoStyle(TEXT_SIZE_SP).copy(textDecoration = TextDecoration.Underline)

        // Act
        val actual = readSceneOf { ScaledBasicText(text = TEXT, style = style) }

        // Assert
        assertEquals(expected = UNSUPPORTED_STYLE, actual = actual)
    }

    @Test
    fun `given a text drawn with a brush, when read, then it is not sendable for its style`() {
        // Arrange
        val style = robotoStyle(TEXT_SIZE_SP).copy(brush = GRADIENT)

        // Act
        val actual = readSceneOf { ScaledBasicText(text = TEXT, style = style) }

        // Assert
        assertEquals(expected = UNSUPPORTED_STYLE, actual = actual)
    }

    @Test
    fun `given a text with a shadow, when read, then it is not sendable for its style`() {
        // Arrange
        val shadow = Shadow(
            color = Color.Black,
            offset = Offset(x = SHADOW_OFFSET_PX, y = SHADOW_OFFSET_PX),
            blurRadius = SHADOW_BLUR_PX,
        )
        val style = robotoStyle(TEXT_SIZE_SP).copy(shadow = shadow)

        // Act
        val actual = readSceneOf { ScaledBasicText(text = TEXT, style = style) }

        // Assert
        assertEquals(expected = UNSUPPORTED_STYLE, actual = actual)
    }

    @Test
    fun `given a superscript text, when read, then it is not sendable for its style`() {
        // Arrange
        val style = robotoStyle(TEXT_SIZE_SP).copy(baselineShift = BaselineShift.Superscript)

        // Act
        val actual = readSceneOf { ScaledBasicText(text = TEXT, style = style) }

        // Assert
        assertEquals(expected = UNSUPPORTED_STYLE, actual = actual)
    }

    @Test
    fun `given an indented text, when read, then it is not sendable for its style`() {
        // Arrange
        val style = robotoStyle(TEXT_SIZE_SP).copy(textIndent = TextIndent(firstLine = INDENT_SP.sp))

        // Act
        val actual = readSceneOf { ScaledBasicText(text = TEXT, style = style) }

        // Assert
        assertEquals(expected = UNSUPPORTED_STYLE, actual = actual)
    }

    @Test
    fun `given a text with its own background color, when read, then it is not sendable for its style`() {
        // Arrange
        val style = robotoStyle(sizeSp = TEXT_SIZE_SP).copy(background = Color.Yellow)

        // Act
        val actual = readSceneOf { ScaledBasicText(text = TEXT, style = style) }

        // Assert
        assertEquals(expected = UNSUPPORTED_STYLE, actual = actual)
    }

    @Test
    fun `given a stretched text, when read, then it is not sendable for its style`() {
        // Arrange
        val stretch = TextGeometricTransform(scaleX = STRETCH_X)
        val style = robotoStyle(sizeSp = TEXT_SIZE_SP).copy(textGeometricTransform = stretch)

        // Act
        val actual = readSceneOf { ScaledBasicText(text = TEXT, style = style) }

        // Assert
        assertEquals(expected = UNSUPPORTED_STYLE, actual = actual)
    }

    @Test
    fun `given a text drawn as an outline, when read, then it is not sendable for its style`() {
        // Arrange
        val style = robotoStyle(sizeSp = TEXT_SIZE_SP).copy(drawStyle = Stroke(width = STROKE_WIDTH_PX))

        // Act
        val actual = readSceneOf { ScaledBasicText(text = TEXT, style = style) }

        // Assert
        assertEquals(expected = UNSUPPORTED_STYLE, actual = actual)
    }
}
