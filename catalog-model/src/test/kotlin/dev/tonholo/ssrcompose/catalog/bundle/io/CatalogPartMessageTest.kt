package dev.tonholo.ssrcompose.catalog.bundle.io

import dev.tonholo.ssrcompose.catalog.bundle.CatalogFileException
import dev.tonholo.ssrcompose.catalog.bundle.readPart
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CatalogPartMessageTest {
    @Test
    fun `given a very long unknown key, when the file is refused, then the message is short`() {
        // Arrange
        val key = "k".repeat(n = VERY_LONG)
        val text = """{"format":1,"part":"app","choices":[],"components":[],"$key":1}"""

        // Act
        val read = { readPart(text = text) }

        // Assert
        assertShort(message = assertFailsWith<CatalogFileException> { read() }.message)
    }

    @Test
    fun `given a very long component name, when the file is refused, then the message is short and marked`() {
        // Arrange
        val prefix = "a".repeat(n = PREFIX_LENGTH)
        val name = prefix + "Z".repeat(n = VERY_LONG - PREFIX_LENGTH)
        val text = """{"format":1,"part":"app","choices":[],"components":[{"name":"$name","parameters":[]}]}"""

        // Act
        val read = { readPart(text = text) }

        // Assert
        assertShortAndMarked(message = assertFailsWith<CatalogFileException> { read() }.message, prefix = prefix)
    }

    @Test
    fun `given a missing field, when the file is refused, then the message names it`() {
        // Arrange
        val text = """{"format":1,"part":"app","components":[]}"""

        // Act
        val read = { readPart(text = text) }

        // Assert
        val error = assertFailsWith<CatalogFileException> { read() }
        assertTrue(actual = error.message.orEmpty().contains(other = "choices"))
    }

    @Test
    fun `given a value of the wrong type, when the file is refused, then the message holds the path to it`() {
        // Arrange
        val text = """{"format":1,"part":5,"choices":[],"components":[]}"""

        // Act
        val read = { readPart(text = text) }

        // Assert
        val error = assertFailsWith<CatalogFileException> { read() }
        assertTrue(actual = error.message.orEmpty().contains(other = "\$.part"))
    }

    private fun assertShort(message: String?) {
        val text = message.orEmpty()
        assertTrue(actual = text.length < MESSAGE_LIMIT, message = "The message has ${text.length} characters")
    }

    private fun assertShortAndMarked(message: String?, prefix: String) {
        assertShort(message = message)
        val text = message.orEmpty()
        val cutStart = text.indexOf(string = prefix)
        assertTrue(actual = cutStart >= 0, message = text)
        val echoed = text.substring(startIndex = cutStart).removeSuffix(suffix = "\"")
        assertTrue(actual = echoed.length > PREFIX_LENGTH, message = text)
        assertFalse(actual = echoed.contains(other = "Z"), message = text)
    }

    private companion object {
        const val PREFIX_LENGTH = 64
        const val VERY_LONG = 100_000
        const val MESSAGE_LIMIT = 512
    }
}
