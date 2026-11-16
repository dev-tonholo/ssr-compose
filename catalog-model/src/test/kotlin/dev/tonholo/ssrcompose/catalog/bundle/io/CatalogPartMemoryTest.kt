package dev.tonholo.ssrcompose.catalog.bundle.io

import dev.tonholo.ssrcompose.catalog.bundle.CatalogFileException
import dev.tonholo.ssrcompose.catalog.bundle.DEFAULT_CAP_BYTES
import dev.tonholo.ssrcompose.catalog.bundle.partText
import dev.tonholo.ssrcompose.catalog.bundle.readPart
import kotlin.test.Test
import kotlin.test.assertFailsWith

class CatalogPartMemoryTest {
    @Test
    fun `given a parameter padded with a huge unknown field, when the file is read, then it is refused`() {
        // Arrange
        val parameter = """{"name":"title","kind":"text","required":true,"padding":$HUGE_ARRAY}"""

        // Act
        val read = { readPart(text = partText(parameter = parameter)) }

        // Assert
        assertFailsWith<CatalogFileException> { read() }
    }

    @Test
    fun `given a huge array as the format, when the file is read, then it is refused`() {
        // Arrange
        val text = """{"format":$HUGE_ARRAY,"part":"app","choices":[],"components":[]}"""

        // Act
        val read = { readPart(text = text) }

        // Assert
        assertFailsWith<CatalogFileException> { read() }
    }

    @Test
    fun `given a huge array as a flag, when the file is read, then it is refused`() {
        // Arrange
        val parameter = """{"name":"title","kind":"text","required":$HUGE_ARRAY}"""

        // Act
        val read = { readPart(text = partText(parameter = parameter)) }

        // Assert
        assertFailsWith<CatalogFileException> { read() }
    }

    private companion object {
        const val ENVELOPE_LENGTH = 1_024
        const val PADDING_ENTRY = "1,"
        val PADDING_ENTRIES = (DEFAULT_CAP_BYTES - ENVELOPE_LENGTH) / PADDING_ENTRY.length
        val HUGE_ARRAY = "[" + PADDING_ENTRY.repeat(n = PADDING_ENTRIES) + "1]"
    }
}
