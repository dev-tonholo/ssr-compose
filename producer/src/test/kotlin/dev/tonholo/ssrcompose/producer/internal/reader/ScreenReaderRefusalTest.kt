package dev.tonholo.ssrcompose.producer.internal.reader

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import dev.tonholo.ssrcompose.producer.ScaledBasicText
import dev.tonholo.ssrcompose.producer.testing.readSceneOf
import dev.tonholo.ssrcompose.producer.testing.robotoStyle
import kotlin.test.Test
import kotlin.test.assertEquals

private const val SIZE_DP = 10
private const val PADDING_DP = 2
private const val BORDER_DP = 1
private const val CORNER_DP = 2
private const val OTHER_CORNER_DP = 3
private const val TEXT_SIZE_SP = 14
private const val LAZY_ROWS = 3
private const val TEXT = "text"
private val GRADIENT = Brush.verticalGradient(colors = listOf(Color.Red, Color.Blue))

class ScreenReaderRefusalTest {
    @Test
    fun `given an image, when read, then it is not sendable as an image`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.Image)

        // Act
        val actual = readSceneOf {
            Image(painter = ColorPainter(Color.Red), contentDescription = null, modifier = Modifier.size(SIZE_DP.dp))
        }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a gradient background, when read, then it is not sendable as a brush`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.Brush)

        // Act
        val actual = readSceneOf { Box(Modifier.size(SIZE_DP.dp).background(brush = GRADIENT)) }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a gradient border, when read, then it is not sendable as a brush`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.Brush)

        // Act
        val actual = readSceneOf {
            Box(Modifier.size(SIZE_DP.dp).border(width = BORDER_DP.dp, brush = GRADIENT, shape = RoundedCornerShape(0)))
        }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a lazy column, when read, then it is not sendable as a lazy list`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.LazyList)

        // Act
        val actual = readSceneOf {
            LazyColumn { items(count = LAZY_ROWS) { ScaledBasicText(text = TEXT, style = robotoStyle(TEXT_SIZE_SP)) } }
        }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a background with cut corners, when read, then it is not sendable as an unknown shape`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.UnknownShape)

        // Act
        val actual = readSceneOf {
            Box(Modifier.size(SIZE_DP.dp).background(color = Color.Red, shape = CutCornerShape(CORNER_DP.dp)))
        }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a text with no font family, when read, then it is not sendable for having no family`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.NoFontFamily)

        // Act
        val actual = readSceneOf { ScaledBasicText(text = TEXT) }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a text in FontFamily Default, when read, then it is not sendable for having no family`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.NoFontFamily)

        // Act
        val actual = readSceneOf { ScaledBasicText(text = TEXT, style = TextStyle(fontFamily = FontFamily.Default)) }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a text in a family that is not the bundled Roboto, when read, then it is not sendable`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.UnknownFontFamily)

        // Act
        val actual = readSceneOf { ScaledBasicText(text = TEXT, style = TextStyle(fontFamily = FontFamily.Serif)) }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a text with a bold span, when read, then it is not sendable for several styles`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.SeveralStyles)
        val text = buildAnnotatedString {
            append(TEXT)
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(TEXT) }
        }

        // Act
        val actual = readSceneOf { ScaledBasicText(text = text, style = robotoStyle(TEXT_SIZE_SP)) }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a text with its own background, when read, then it is not sendable as a text with decoration`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.TextWithDecoration)

        // Act
        val actual = readSceneOf {
            ScaledBasicText(
                text = TEXT,
                modifier = Modifier.background(Color.Red).padding(PADDING_DP.dp),
                style = robotoStyle(TEXT_SIZE_SP),
            )
        }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a scroll container with its own background, when read, then it is not sendable`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.ScrollWithDecoration)

        // Act
        val actual = readSceneOf {
            Column(Modifier.size(SIZE_DP.dp).background(Color.Red).verticalScroll(rememberScrollState())) {
                Box(Modifier.size(SIZE_DP.dp))
            }
        }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a padding between a background and a border, when read, then it is not sendable as decorations apart`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.DecorationsApart)

        // Act
        val actual = readSceneOf {
            Box(
                Modifier
                    .size(SIZE_DP.dp)
                    .background(Color.Red)
                    .padding(PADDING_DP.dp)
                    .border(width = BORDER_DP.dp, color = Color.Black),
            )
        }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a background and a border with other corners, when read, then they are decorations apart`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.DecorationsApart)

        // Act
        val actual = readSceneOf {
            Box(
                Modifier
                    .size(SIZE_DP.dp)
                    .background(color = Color.Red, shape = RoundedCornerShape(CORNER_DP.dp))
                    .border(width = BORDER_DP.dp, color = Color.Black, shape = RoundedCornerShape(OTHER_CORNER_DP.dp)),
            )
        }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a box marked as an image with nothing painted, when read, then it is not sendable as an image`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.Image)

        // Act
        val actual = readSceneOf { Box(Modifier.size(SIZE_DP.dp).semantics { role = Role.Image }) }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a box with two backgrounds, when read, then they are decorations apart`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.DecorationsApart)

        // Act
        val actual = readSceneOf { Box(Modifier.size(SIZE_DP.dp).background(Color.Red).background(Color.Blue)) }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a box with two borders, when read, then they are decorations apart`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.DecorationsApart)

        // Act
        val actual = readSceneOf {
            Box(
                Modifier
                    .size(SIZE_DP.dp)
                    .border(width = BORDER_DP.dp, color = Color.Black)
                    .border(width = BORDER_DP.dp, color = Color.Red),
            )
        }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a popup, when read, then it is not sendable for opening a second window`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.SeveralWindows)

        // Act
        val actual = readSceneOf {
            Box(Modifier.size(SIZE_DP.dp))
            Popup { ScaledBasicText(text = TEXT, style = robotoStyle(TEXT_SIZE_SP)) }
        }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a dialog, when read, then it is not sendable for opening a second window`() {
        // Arrange
        val expected = ScreenRead.NotSendable(reason = NotSendableReason.SeveralWindows)

        // Act
        val actual = readSceneOf {
            Box(Modifier.size(SIZE_DP.dp))
            Dialog(onDismissRequest = {}) { ScaledBasicText(text = TEXT, style = robotoStyle(TEXT_SIZE_SP)) }
        }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }
}
