package dev.tonholo.ssrcompose.producer.text.layoutlib

import kotlin.test.Test
import kotlin.test.assertEquals

private const val LATIN_FONT_CASE = "roboto-regular/latin-short/24sp/x1.3"
private const val FIGURE_SPACE = "\u2007"

class LayoutlibTextEngineLineEndTest {
    private val engine = testEngine()
    private val row = DeviceRecording.tablet().row(LATIN_FONT_CASE)
    private val request = row.request(engine.loadFont(classpathBytes(row.fontFile)))

    @Test
    fun `given spaces at the end of the text, when measured, then the last line ends before them as in Compose`() {
        // Arrange
        val withTrailingSpaces = request.copy(text = "Hello     ")

        // Act
        val measured = engine.measure(withTrailingSpaces)

        // Assert
        assertEquals(expected = listOf(5), actual = measured.lineEnds)
    }

    @Test
    fun `given a tab or a figure space ending a line, when measured, then Compose keeps it as visible`() {
        // Arrange
        val withKeptCharacters = request.copy(text = "ab\t\ncd$FIGURE_SPACE")

        // Act
        val measured = engine.measure(withKeptCharacters)

        // Assert
        assertEquals(expected = listOf(3, 7), actual = measured.lineEnds)
    }

    @Test
    fun `given an ogham or ideographic space ending a line, when measured, then the line ends before it as in Compose`() {
        // Arrange
        val withOtherSpaces = request.copy(text = "ab\u1680\ncd\u3000")

        // Act
        val measured = engine.measure(withOtherSpaces)

        // Assert
        assertEquals(expected = listOf(2, 6), actual = measured.lineEnds)
    }

    @Test
    fun `given an en quad or a hair space ending a line, when measured, then the line ends before it as in Compose`() {
        // Arrange
        val withTypographicSpaces = request.copy(text = "ab\u2000\ncd\u200A")

        // Act
        val measured = engine.measure(withTypographicSpaces)

        // Assert
        assertEquals(expected = listOf(2, 6), actual = measured.lineEnds)
    }

    @Test
    fun `given a mathematical space ending a line, when measured, then the line ends before it as in Compose`() {
        // Arrange
        val withMathematicalSpace = request.copy(text = "ab\u205F\ncd")

        // Act
        val measured = engine.measure(withMathematicalSpace)

        // Assert
        assertEquals(expected = listOf(2, 6), actual = measured.lineEnds)
    }
}
