package dev.tonholo.ssrcompose.producer

import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.tonholo.ssrcompose.producer.internal.text.ScaledTextMark
import dev.tonholo.ssrcompose.producer.testing.LINE_HEIGHT_EM
import dev.tonholo.ssrcompose.producer.testing.LINE_HEIGHT_SP
import dev.tonholo.ssrcompose.producer.testing.SMALL_SIZE_SP
import dev.tonholo.ssrcompose.producer.testing.bareMarkOf
import dev.tonholo.ssrcompose.producer.testing.emulatorSpec
import dev.tonholo.ssrcompose.producer.testing.markOf
import dev.tonholo.ssrcompose.producer.testing.robotoStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ScaledBasicTextMarkTest {
    @Test
    fun `given no producer screen, when composed, then the text has no mark`() {
        // Arrange
        val spec = emulatorSpec()

        // Act
        val actual = bareMarkOf(spec = spec)

        // Assert
        assertNull(actual = actual)
    }

    @Test
    fun `given a curve, when composed, then the text is marked with its size and line height in sp`() {
        // Arrange
        val style = robotoStyle(sizeSp = SMALL_SIZE_SP).copy(lineHeight = LINE_HEIGHT_SP.sp)
        val expected = ScaledTextMark(sizeSp = SMALL_SIZE_SP.toFloat(), lineHeightSp = LINE_HEIGHT_SP.toFloat())

        // Act
        val actual = markOf(spec = emulatorSpec(), style = style)

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a line height in em, when composed, then the mark holds no line height`() {
        // Arrange
        val style = robotoStyle(sizeSp = SMALL_SIZE_SP).copy(lineHeight = LINE_HEIGHT_EM.em)
        val expected = ScaledTextMark(sizeSp = SMALL_SIZE_SP.toFloat(), lineHeightSp = null)

        // Act
        val actual = markOf(spec = emulatorSpec(), style = style)

        // Assert
        assertEquals(expected = expected, actual = actual)
    }
}
