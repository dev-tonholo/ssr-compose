package dev.tonholo.ssrcompose.catalog.validation

import kotlin.test.Test
import kotlin.test.assertEquals

class EchoedTextTest {
    @Test
    fun `given a text within the limit, when it is cut for a message, then it is unchanged`() {
        // Arrange
        val texts = listOf("", "Card", "a".repeat(n = EchoedText.MAX_LENGTH))

        // Act
        val actual = texts.map { it.cutForMessage() }

        // Assert
        assertEquals(expected = texts, actual = actual)
    }

    @Test
    fun `given a text one character over the limit, when it is cut for a message, then the marker is added`() {
        // Arrange
        val text = "a".repeat(n = EchoedText.MAX_LENGTH + 1)

        // Act
        val actual = text.cutForMessage()

        // Assert
        assertEquals(expected = "a".repeat(n = EchoedText.MAX_LENGTH) + EchoedText.CUT_MARKER, actual = actual)
    }
}
