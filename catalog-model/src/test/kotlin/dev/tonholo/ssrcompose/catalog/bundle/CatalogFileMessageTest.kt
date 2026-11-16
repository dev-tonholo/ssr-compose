package dev.tonholo.ssrcompose.catalog.bundle

import dev.tonholo.ssrcompose.catalog.validation.EchoedText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CatalogFileMessageTest {
    @Test
    fun `given a very long unknown key, when the file is refused, then the message is short and has no cause`() {
        // Arrange
        val key = "k".repeat(n = VERY_LONG)
        val text = """{"format":1,"version":"1","choices":[],"components":[],"$key":1}"""

        // Act
        val read = { CatalogFile.read(text = text) }

        // Assert
        val error = assertFailsWith<CatalogFileException> { read() }
        assertShort(message = error.message)
        assertNull(actual = error.cause)
    }

    @Test
    fun `given a very long component name, when the file is refused, then the message is short and marked`() {
        // Arrange
        val name = "a".repeat(n = VERY_LONG)
        val text = """{"format":1,"version":"1","choices":[],"components":[{"name":"$name","parameters":[]}]}"""

        // Act
        val read = { CatalogFile.read(text = text) }

        // Assert
        val error = assertFailsWith<CatalogFileException> { read() }
        assertShortAndMarked(message = error.message)
        assertNull(actual = error.cause)
    }

    @Test
    fun `given a very long text that is not JSON, when the file is refused, then the message is short`() {
        // Arrange
        val text = "x".repeat(n = VERY_LONG)

        // Act
        val read = { CatalogFile.read(text = text) }

        // Assert
        val error = assertFailsWith<CatalogFileException> { read() }
        assertShort(message = error.message)
        assertNull(actual = error.cause)
    }

    @Test
    fun `given a missing field, when the file is refused, then the message names it and there is no cause`() {
        // Arrange
        val text = """{"format":1,"choices":[],"components":[]}"""

        // Act
        val read = { CatalogFile.read(text = text) }

        // Assert
        val error = assertFailsWith<CatalogFileException> { read() }
        assertTrue(actual = error.message.orEmpty().contains(other = "version"))
        assertNull(actual = error.cause)
    }

    @Test
    fun `given a value of the wrong type, when the file is refused, then the message holds the path to it`() {
        // Arrange
        val text = """{"format":1,"version":5,"choices":[],"components":[]}"""

        // Act
        val read = { CatalogFile.read(text = text) }

        // Assert
        val error = assertFailsWith<CatalogFileException> { read() }
        assertTrue(actual = error.message.orEmpty().contains(other = "\$.version"))
        assertNull(actual = error.cause)
    }

    @Test
    fun `given an unknown format, when the file is refused, then the message is the known one`() {
        // Arrange
        val text = """{"format":2,"version":"1","choices":[],"components":[]}"""

        // Act
        val read = { CatalogFile.read(text = text) }

        // Assert
        val error = assertFailsWith<CatalogFileException> { read() }
        assertEquals(expected = "The catalog file format 2 is not known", actual = error.message)
        assertNull(actual = error.cause)
    }

    private fun assertShort(message: String?) {
        val text = message.orEmpty()
        assertTrue(actual = text.length < MESSAGE_LIMIT, message = "The message has ${text.length} characters")
    }

    private fun assertShortAndMarked(message: String?) {
        assertShort(message = message)
        assertTrue(actual = message.orEmpty().contains(other = EchoedText.CUT_MARKER))
    }

    private companion object {
        const val VERY_LONG = 100_000
        const val MESSAGE_LIMIT = 512
    }
}
