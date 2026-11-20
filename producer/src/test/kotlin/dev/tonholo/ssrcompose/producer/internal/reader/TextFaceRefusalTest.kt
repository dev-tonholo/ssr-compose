package dev.tonholo.ssrcompose.producer.internal.reader

import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import dev.tonholo.ssrcompose.producer.testing.readSceneOf
import dev.tonholo.ssrcompose.producer.testing.robotoStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private const val TEXT = "text"
private const val TEXT_SIZE_SP = 14
private const val LIGHT = 300
private const val REGULAR = 400
private const val MEDIUM = 500
private const val BOLD = 700
private val BUNDLED_FACES = listOf(
    REGULAR to FontStyle.Normal,
    MEDIUM to FontStyle.Normal,
    BOLD to FontStyle.Normal,
    REGULAR to FontStyle.Italic,
)
private val UNAVAILABLE_FACE = ScreenRead.NotSendable(reason = NotSendableReason.UnavailableFontFace)

class TextFaceRefusalTest {
    @Test
    fun `given each face the bundled Roboto has a file for, when read, then it is sent`() {
        // Arrange
        val faces = BUNDLED_FACES

        // Act
        val actual = faces.map { (weight, style) ->
            readSceneOf {
                BasicText(
                    text = TEXT,
                    style = robotoStyle(sizeSp = TEXT_SIZE_SP, weight = FontWeight(weight)).copy(fontStyle = style),
                )
            }
        }

        // Assert
        actual.forEach { assertIs<ScreenRead.Document>(value = it) }
    }

    @Test
    fun `given a weight with no file, when read, then it is not sendable for an unavailable face`() {
        // Arrange
        val style = robotoStyle(sizeSp = TEXT_SIZE_SP, weight = FontWeight(LIGHT))

        // Act
        val actual = readSceneOf { BasicText(text = TEXT, style = style) }

        // Assert
        assertEquals(expected = UNAVAILABLE_FACE, actual = actual)
    }

    @Test
    fun `given an italic in a weight with no italic file, when read, then it is not sendable for its face`() {
        // Arrange
        val style = robotoStyle(sizeSp = TEXT_SIZE_SP, weight = FontWeight(MEDIUM)).copy(fontStyle = FontStyle.Italic)

        // Act
        val actual = readSceneOf { BasicText(text = TEXT, style = style) }

        // Assert
        assertEquals(expected = UNAVAILABLE_FACE, actual = actual)
    }
}
