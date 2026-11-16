package dev.tonholo.ssrcompose.catalog.bundle

import dev.tonholo.ssrcompose.catalog.validation.CatalogLimits
import kotlin.test.Test
import kotlin.test.assertFailsWith

class CatalogFileMemoryTest {
    @Test
    fun `given a parameter padded with a huge unknown field, when the file is read, then it is refused`() {
        // Arrange
        val parameter = """{"name":"title","kind":"text","required":true,"padding":$HUGE_ARRAY}"""

        // Act
        val read = { CatalogFile.read(text = component(parameter = parameter)) }

        // Assert
        assertFailsWith<CatalogFileException> { read() }
    }

    @Test
    fun `given a callback with a huge list of arguments, when the file is read, then it is refused`() {
        // Arrange
        val arguments = """{"kind":"text"},""".repeat(n = ARGUMENT_ENTRIES) + """{"kind":"text"}"""
        val parameter = """{"name":"onTap","kind":"callback","arguments":[$arguments],"required":true}"""

        // Act
        val read = { CatalogFile.read(text = component(parameter = parameter)) }

        // Assert
        assertFailsWith<CatalogFileException> { read() }
    }

    @Test
    fun `given a huge array as the format, when the file is read, then it is refused`() {
        // Arrange
        val text = """{"format":$HUGE_ARRAY,"version":"1","choices":[],"components":[]}"""

        // Act
        val read = { CatalogFile.read(text = text) }

        // Assert
        assertFailsWith<CatalogFileException> { read() }
    }

    @Test
    fun `given a huge array as required, when the file is read, then it is refused`() {
        // Arrange
        val parameter = """{"name":"title","kind":"text","required":$HUGE_ARRAY}"""

        // Act
        val read = { CatalogFile.read(text = component(parameter = parameter)) }

        // Assert
        assertFailsWith<CatalogFileException> { read() }
    }

    @Test
    fun `given a huge array as nullable, when the file is read, then it is refused`() {
        // Arrange
        val parameter = """{"name":"title","kind":"text","required":true,"nullable":$HUGE_ARRAY}"""

        // Act
        val read = { CatalogFile.read(text = component(parameter = parameter)) }

        // Assert
        assertFailsWith<CatalogFileException> { read() }
    }

    private fun component(parameter: String): String =
        """{"format":1,"version":"1","choices":[],"components":[{"name":"Card","parameters":[$parameter]}]}"""

    private companion object {
        const val ENVELOPE_LENGTH = 1_024
        const val PADDING_ENTRY = "1,"
        const val ARGUMENT_ENTRY = """{"kind":"text"},"""
        const val PADDING_ENTRIES = (CatalogLimits.MAX_FILE_LENGTH - ENVELOPE_LENGTH) / PADDING_ENTRY.length
        const val ARGUMENT_ENTRIES = (CatalogLimits.MAX_FILE_LENGTH - ENVELOPE_LENGTH) / ARGUMENT_ENTRY.length
        val HUGE_ARRAY = "[" + PADDING_ENTRY.repeat(n = PADDING_ENTRIES) + "1]"
    }
}
