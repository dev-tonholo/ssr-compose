package dev.tonholo.ssrcompose.producer.internal.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.tonholo.ssrcompose.producer.testing.readSceneOf
import dev.tonholo.ssrcompose.producer.testing.robotoStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private const val BOX_DP = 10
private const val NEGATIVE_BORDER_DP = -1
private const val BORDER_DP = 1
private const val TEXT = "text"
private const val TEXT_SIZE_SP = 14
private const val OVERSIZED_TEXT_SP = 1_001
private const val FAR_OFFSET_DP = 100_001
private val OUT_OF_BOUNDS = ScreenRead.NotSendable(reason = NotSendableReason.OutOfBounds)

class OutOfBoundsRefusalTest {
    @Test
    fun `given a border of negative width, when read, then it is not sendable as out of bounds`() {
        // Arrange
        val width = NEGATIVE_BORDER_DP.dp

        // Act
        val actual = readSceneOf { Box(Modifier.size(BOX_DP.dp).border(width = width, color = Color.Black)) }

        // Assert
        assertEquals(expected = OUT_OF_BOUNDS, actual = actual)
    }

    @Test
    fun `given a border of a valid width, when read, then it is sent`() {
        // Arrange
        val width = BORDER_DP.dp

        // Act
        val actual = readSceneOf { Box(Modifier.size(BOX_DP.dp).border(width = width, color = Color.Black)) }

        // Assert
        assertIs<ScreenRead.Document>(value = actual)
    }

    @Test
    fun `given a text larger than the document holds, when read, then it is not sendable as out of bounds`() {
        // Arrange
        val style = robotoStyle(sizeSp = OVERSIZED_TEXT_SP)

        // Act
        val actual = readSceneOf { BasicText(text = TEXT, style = style) }

        // Assert
        assertEquals(expected = OUT_OF_BOUNDS, actual = actual)
    }

    @Test
    fun `given a box placed beyond what the document holds, when read, then it is not sendable as out of bounds`() {
        // Arrange
        val offset = FAR_OFFSET_DP.dp

        // Act
        val actual = readSceneOf {
            Box(Modifier.offset(x = offset).size(BOX_DP.dp).background(Color.Red))
        }

        // Assert
        assertEquals(expected = OUT_OF_BOUNDS, actual = actual)
    }

    @Test
    fun `given a text of a valid size, when read, then it is sent`() {
        // Arrange
        val style = robotoStyle(sizeSp = TEXT_SIZE_SP)

        // Act
        val actual = readSceneOf { BasicText(text = TEXT, style = style) }

        // Assert
        assertIs<ScreenRead.Document>(value = actual)
    }
}
