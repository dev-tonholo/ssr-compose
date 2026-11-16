package dev.tonholo.ssrcompose.core.catalog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CatalogVersionTest {
    @Test
    fun `given an empty text, when a CatalogVersion is created, then it is rejected`() {
        // Arrange
        val text = ""

        // Act
        val create = { CatalogVersion(value = text) }

        // Assert
        assertFailsWith<IllegalArgumentException> { create() }
    }

    @Test
    fun `given a text over the maximum length, when a CatalogVersion is created, then it is rejected`() {
        // Arrange
        val text = "v".repeat(n = MAX_LENGTH + 1)

        // Act
        val create = { CatalogVersion(value = text) }

        // Assert
        assertFailsWith<IllegalArgumentException> { create() }
    }

    @Test
    fun `given a text of the maximum length, when a CatalogVersion is created, then it is accepted`() {
        // Arrange
        val text = "v".repeat(n = MAX_LENGTH)

        // Act
        val actual = CatalogVersion(value = text)

        // Assert
        assertEquals(expected = text, actual = actual.value)
    }

    @Test
    fun `given a text with a control character, when a CatalogVersion is created, then it is rejected`() {
        // Arrange
        val text = "3\n4"

        // Act
        val create = { CatalogVersion(value = text) }

        // Assert
        assertFailsWith<IllegalArgumentException> { create() }
    }

    @Test
    fun `given a text with an unpaired surrogate, when a CatalogVersion is created, then it is rejected`() {
        // Arrange
        val texts = listOf("\uD800", "\uDC00", "\uD800a", "\uDC00\uD800", "\uD83D\uDE00\uD800")

        // Act
        val notRejected = texts.filter { text ->
            runCatching { CatalogVersion(value = text) }.exceptionOrNull() !is IllegalArgumentException
        }

        // Assert
        assertEquals(expected = emptyList(), actual = notRejected)
    }

    @Test
    fun `given a text with a valid surrogate pair, when a CatalogVersion is created, then it is accepted`() {
        // Arrange
        val text = "1\uD83D\uDE002"

        // Act
        val actual = CatalogVersion(value = text)

        // Assert
        assertEquals(expected = text, actual = actual.value)
    }

    private companion object {
        const val MAX_LENGTH = 64
    }
}
