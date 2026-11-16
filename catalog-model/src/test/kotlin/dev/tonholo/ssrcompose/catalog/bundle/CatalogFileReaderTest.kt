package dev.tonholo.ssrcompose.catalog.bundle

import dev.tonholo.ssrcompose.catalog.validation.CatalogLimits
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CatalogFileReaderTest {
    @Test
    fun `given a file exactly at the length cap, when it is read, then it is accepted`() {
        // Arrange
        val text = MINIMAL_FILE + " ".repeat(n = CatalogLimits.MAX_FILE_LENGTH - MINIMAL_FILE.length)

        // Act
        val actual = CatalogFile.read(text = text)

        // Assert
        assertEquals(expected = emptyList(), actual = actual.components)
    }

    @Test
    fun `given a caller cap, when a longer file is read, then it is refused and a file at the cap is read`() {
        // Arrange
        val cap = MINIMAL_FILE.length + SMALL_CAP_MARGIN
        val atCap = MINIMAL_FILE + " ".repeat(n = SMALL_CAP_MARGIN)
        val overCap = atCap + " "

        // Act
        val read = CatalogFile.read(text = atCap, maxLength = cap)
        val readOver = { CatalogFile.read(text = overCap, maxLength = cap) }

        // Assert
        assertEquals(expected = emptyList(), actual = read.components)
        val error = assertFailsWith<CatalogFileException> { readOver() }
        assertTrue(actual = error.message.orEmpty().contains(other = "longer than $cap"))
    }

    @Test
    fun `given a text over the length cap that is not a catalog, when it is read, then it is refused before parsing`() {
        // Arrange
        val text = " ".repeat(n = CatalogLimits.MAX_FILE_LENGTH + 1)

        // Act
        val read = { CatalogFile.read(text = text) }

        // Assert
        val error = assertFailsWith<CatalogFileException> { read() }
        assertTrue(actual = error.message.orEmpty().contains(other = "longer than"))
    }

    @Test
    fun `given more components than the old cap, when the file is read, then all are read`() {
        // Arrange
        val text = fileWithComponents(count = MANY)

        // Act
        val actual = CatalogFile.read(text = text)

        // Assert
        assertEquals(expected = MANY, actual = actual.components.size)
    }

    @Test
    fun `given more parameters than the old cap, when the file is read, then all are read`() {
        // Arrange
        val text = fileWithParameters(count = MANY)

        // Act
        val actual = CatalogFile.read(text = text)

        // Assert
        assertEquals(expected = MANY, actual = actual.components.single().parameters.size)
    }

    @Test
    fun `given values of the wrong type, when the file is read, then each file is refused`() {
        // Arrange
        val parameter = """{"name":"title","kind":"text","required":true}"""
        val wrongFiles = listOf(
            """[]""",
            """{"format":"1","version":"1","choices":[],"components":[]}""",
            """{"format":1.5,"version":"1","choices":[],"components":[]}""",
            """{"format":1,"version":1,"choices":[],"components":[]}""",
            """{"format":1,"version":"1","choices":{},"components":[]}""",
            """{"format":1,"version":"1","choices":[],"components":{}}""",
            """{"format":1,"version":"1","choices":[],"components":[1]}""",
            """{"format":1,"version":"1","choices":[{"name":"A","entries":"x"}],"components":[]}""",
            component(parameter = """{"name":5,"kind":"text","required":true}"""),
            component(parameter = """{"name":"title","kind":5,"required":true}"""),
            component(parameter = """{"name":"title","kind":"text","required":"true"}"""),
            component(parameter = """{"name":"title","kind":"text","required":true,"nullable":"yes"}"""),
            component(parameter = """{"name":"onTap","kind":"callback","arguments":"text","required":true}"""),
            component(parameter = """{"name":"onTap","kind":"callback","arguments":[5],"required":true}"""),
            component(parameter = """{"name":"onTap","kind":"callback","arguments":["text"],"required":true}"""),
            component(parameter = """{"name":"items","kind":"list","of":"text","required":true}"""),
            component(parameter = """{"name":"content","kind":"slot","accepts":1,"required":true}"""),
            component(parameter = parameter.replace(oldValue = "text", newValue = "list")),
        )

        // Act
        val notRefused = wrongFiles.filter { file ->
            runCatching { CatalogFile.read(text = file) }.exceptionOrNull() !is CatalogFileException
        }

        // Assert
        assertEquals(expected = emptyList(), actual = notRefused)
    }

    private fun component(parameter: String): String =
        """{"format":1,"version":"1","choices":[],"components":[{"name":"Card","parameters":[$parameter]}]}"""

    private fun fileWithComponents(count: Int): String {
        val components = List(size = count) { index -> """{"name":"C$index","parameters":[]}""" }
        return """{"format":1,"version":"1","choices":[],"components":[${components.joinToString(separator = ",")}]}"""
    }

    private fun fileWithParameters(count: Int): String {
        val parameters = List(size = count) { index -> """{"name":"p$index","kind":"text","required":true}""" }
        val component = """{"name":"Card","parameters":[${parameters.joinToString(separator = ",")}]}"""
        return """{"format":1,"version":"1","choices":[],"components":[$component]}"""
    }

    private companion object {
        const val MANY = 2_000
        const val SMALL_CAP_MARGIN = 10
        const val MINIMAL_FILE = """{"format":1,"version":"1","choices":[],"components":[]}"""
    }
}
