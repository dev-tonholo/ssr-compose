package dev.tonholo.ssrcompose.catalog.validation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EchoedTextTest {
    @Test
    fun `given a text within the limit, when it is cut for a message, then it is unchanged`() {
        // Arrange
        val texts = listOf("", "Card", "a".repeat(n = LIMIT))

        // Act
        val actual = texts.map { it.cutForMessage() }

        // Assert
        assertEquals(expected = texts, actual = actual)
    }

    @Test
    fun `given a text over the limit, when it is cut for a message, then it keeps the prefix and is longer than it`() {
        // Arrange
        val prefix = "a".repeat(n = LIMIT)
        val text = "${prefix}b"

        // Act
        val actual = text.cutForMessage()

        // Assert
        assertTrue(actual = actual.startsWith(prefix = prefix), message = actual)
        assertTrue(actual = actual.length > LIMIT, message = actual)
        assertFalse(actual = actual.contains(other = "b"), message = actual)
    }

    private companion object {
        const val LIMIT = 64
    }
}
