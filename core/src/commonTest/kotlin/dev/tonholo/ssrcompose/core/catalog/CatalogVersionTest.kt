package dev.tonholo.ssrcompose.core.catalog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

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
    fun `given a text with a valid surrogate pair, when a CatalogVersion is created, then it is rejected`() {
        // Arrange
        val text = "1\uD83D\uDE002"

        // Act
        val create = { CatalogVersion(value = text) }

        // Assert
        assertFailsWith<IllegalArgumentException> { create() }
    }

    @Test
    fun `given a label with a separator or a dot edge, when a CatalogVersion is created, then it is rejected`() {
        // Arrange
        val labels = listOf("1/../2", "..", ".", "a/b", "a\\b", "1.", ".1", "1 2")

        // Act
        val accepted = labels.filter(::isAccepted)

        // Assert
        assertEquals(expected = emptyList(), actual = accepted)
    }

    @Test
    fun `given a label with an uppercase letter, when a CatalogVersion is created, then it is rejected`() {
        // Arrange
        val labels = listOf("A", "Release-1", "rELEASE")

        // Act
        val accepted = labels.filter(::isAccepted)

        // Assert
        assertEquals(expected = emptyList(), actual = accepted)
    }

    @Test
    fun `given a Windows device name alone or before a dot, when a CatalogVersion is created, then it is rejected`() {
        // Arrange
        val labels = listOf("con", "CON", "prn", "aux", "nul", "com1", "com9", "lpt1", "lpt9", "con.txt", "com1.2")

        // Act
        val accepted = labels.filter(::isAccepted)

        // Assert
        assertEquals(expected = emptyList(), actual = accepted)
    }

    @Test
    fun `given a label that only starts like a device name, when a CatalogVersion is created, then it is accepted`() {
        // Arrange
        val labels = listOf("console", "com10", "com0", "lpt0", "con-1", "auxiliary", "1.con")

        // Act
        val rejected = labels.filterNot(::isAccepted)

        // Assert
        assertEquals(expected = emptyList(), actual = rejected)
    }

    @Test
    fun `given a label of allowed characters, when a CatalogVersion is created, then it is accepted`() {
        // Arrange
        val labels = listOf("1", "2.0", "release-1_beta", "a.b.c", "_x", "-x")

        // Act
        val rejected = labels.filterNot(::isAccepted)

        // Assert
        assertEquals(expected = emptyList(), actual = rejected)
    }

    @Test
    fun `given a label one character over the maximum, when a CatalogVersion is created, then it is rejected`() {
        // Arrange
        val text = "a".repeat(n = MAX_LENGTH + 1)

        // Act
        val create = { CatalogVersion(value = text) }

        // Assert
        assertFailsWith<IllegalArgumentException> { create() }
    }

    @Test
    fun `given a rejected label, when a CatalogVersion is created, then the error names the rule and the label`() {
        // Arrange
        val text = "A/b"

        // Act
        val message = assertFailsWith<IllegalArgumentException> { CatalogVersion(value = text) }.message.orEmpty()

        // Assert
        assertTrue(actual = message.contains("lowercase") && message.contains(text), message = message)
    }

    private fun isAccepted(text: String): Boolean = runCatching { CatalogVersion(value = text) }.isSuccess

    private companion object {
        const val MAX_LENGTH = 64
    }
}
