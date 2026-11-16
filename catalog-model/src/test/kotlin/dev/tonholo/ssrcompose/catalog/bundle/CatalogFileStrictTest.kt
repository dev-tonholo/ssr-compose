package dev.tonholo.ssrcompose.catalog.bundle

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CatalogFileStrictTest {
    @Test
    fun `given a format that is not a whole number, when the file is read, then each file is refused`() {
        // Arrange
        val formats = listOf(
            "1.5", "1.0", "1e0", "1E0", "+1", "01", "00", "+01", "4294967297", "true", "null", "[]", "{}",
        )

        // Act
        val notRefused = formats.filter { format ->
            val text = """{"format":$format,"version":"1","choices":[],"components":[]}"""
            runCatching { CatalogFile.read(text = text) }.exceptionOrNull() !is CatalogFileException
        }

        // Assert
        assertEquals(expected = emptyList(), actual = notRefused)
    }

    @Test
    fun `given a flag that is not true or false, when the file is read, then each file is refused`() {
        // Arrange
        val flags = listOf(
            """"required":1""",
            """"required":0""",
            """"required":null""",
            """"required":"false"""",
            """"required":true,"nullable":"true"""",
        )

        // Act
        val notRefused = flags.filter { fields ->
            val text = component(parameter = """{"name":"title","kind":"text",$fields}""")
            runCatching { CatalogFile.read(text = text) }.exceptionOrNull() !is CatalogFileException
        }

        // Assert
        assertEquals(expected = emptyList(), actual = notRefused)
    }

    @Test
    fun `given a field that does not belong to the kind, when the file is read, then each file is refused`() {
        // Arrange
        val parameters = listOf(
            """{"name":"onTap","kind":"callback","choice":"A","required":true}""",
            """{"name":"onTap","kind":"callback","of":{"kind":"text"},"required":true}""",
            """{"name":"onTap","kind":"callback","nullable":true,"required":true}""",
            """{"name":"onTap","kind":"callback","accepts":"any","required":true}""",
            """{"name":"content","kind":"slot","required":true}""",
            """{"name":"content","kind":"slot","accepts":"any","choice":"A","required":true}""",
            """{"name":"content","kind":"slot","accepts":"any","nullable":true,"required":true}""",
            """{"name":"title","kind":"text","arguments":[],"required":true}""",
            """{"name":"title","kind":"text","accepts":"any","required":true}""",
        )

        // Act
        val notRefused = parameters.filter { parameter ->
            runCatching { CatalogFile.read(text = component(parameter = parameter)) }
                .exceptionOrNull() !is CatalogFileException
        }

        // Assert
        assertEquals(expected = emptyList(), actual = notRefused)
    }

    @Test
    fun `given a kind with detail that does not belong to it, when the file is read, then each file is refused`() {
        // Arrange
        val kinds = listOf(
            """{"kind":"text","choice":"A"}""",
            """{"kind":"text","of":{"kind":"text"}}""",
            """{"kind":"choice","choice":"A","of":{"kind":"text"}}""",
            """{"kind":"list","choice":"A","of":{"kind":"text"}}""",
        )
        val parameters = kinds.flatMap { kind ->
            listOf(
                """{"name":"p",${kind.removeSurrounding(prefix = "{", suffix = "}")},"required":true}""",
                """{"name":"onTap","kind":"callback","arguments":[$kind],"required":true}""",
                """{"name":"content","kind":"slot","accepts":"any","arguments":[$kind],"required":true}""",
            )
        } + """{"name":"items","kind":"list","of":{"kind":"text","choice":"A"},"required":true}"""

        // Act
        val notRefused = parameters.filter { parameter ->
            runCatching { CatalogFile.read(text = component(parameter = parameter)) }
                .exceptionOrNull() !is CatalogFileException
        }

        // Assert
        assertEquals(expected = emptyList(), actual = notRefused)
    }

    @Test
    fun `given a slot that accepts an unknown value, when the file is read, then it is refused`() {
        // Arrange
        val text = component(parameter = """{"name":"content","kind":"slot","accepts":"server","required":true}""")

        // Act
        val read = { CatalogFile.read(text = text) }

        // Assert
        assertFailsWith<CatalogFileException> { read() }
    }

    @Test
    fun `given an unknown key at each level, when the file is read, then each file is refused`() {
        // Arrange
        val extra = ""","extra":1"""
        val listArgument = """{"kind":"list","of":{"kind":"text"}$extra}"""
        val choiceElement = """{"kind":"choice","choice":"A"$extra}"""
        val files = listOf(
            """{"format":1,"version":"1","choices":[],"components":[]$extra}""",
            """{"format":1,"version":"1","choices":[{"name":"A","entries":["b"]$extra}],"components":[]}""",
            """{"format":1,"version":"1","choices":[],"components":[{"name":"Card","parameters":[]$extra}]}""",
            component(parameter = """{"name":"title","kind":"text","required":true$extra}"""),
            component(parameter = """{"name":"onTap","kind":"callback","required":true$extra}"""),
            component(parameter = """{"name":"content","kind":"slot","accepts":"any","required":true$extra}"""),
            component(parameter = """{"name":"on","kind":"callback","arguments":[$listArgument],"required":true}"""),
            component(parameter = """{"name":"items","kind":"list","of":$choiceElement,"required":true}"""),
        )

        // Act
        val notRefused = files.filter { file ->
            runCatching { CatalogFile.read(text = file) }.exceptionOrNull() !is CatalogFileException
        }

        // Assert
        assertEquals(expected = emptyList(), actual = notRefused)
    }

    @Test
    fun `given an unknown key with deeply nested lists, when the file is read, then it is refused`() {
        // Arrange
        val extra = nested(levels = DEEP_NESTING)
        val text = """{"format":1,"version":"1","choices":[],"components":[],"extra":$extra}"""

        // Act
        val read = { CatalogFile.read(text = text) }

        // Assert
        assertFailsWith<CatalogFileException> { read() }
    }

    @Test
    fun `given an unknown key, when the file is refused, then the message does not repeat the key`() {
        // Arrange
        val text = """{"format":1,"version":"1","choices":[],"components":[],"secret-marker":1}"""

        // Act
        val read = { CatalogFile.read(text = text) }

        // Assert
        val error = assertFailsWith<CatalogFileException> { read() }
        assertFalse(actual = error.message.orEmpty().contains(other = "secret-marker"))
    }

    @Test
    fun `given a text that is not JSON, when the file is refused, then the message does not repeat the text`() {
        // Arrange
        val text = "secret-marker is not a catalog"

        // Act
        val read = { CatalogFile.read(text = text) }

        // Assert
        val error = assertFailsWith<CatalogFileException> { read() }
        assertFalse(actual = error.message.orEmpty().contains(other = "secret-marker"))
    }

    @Test
    fun `given a kind that is not known, when the file is refused, then the message does not repeat the kind`() {
        // Arrange
        val text = component(parameter = """{"name":"title","kind":"secret-marker","required":true}""")

        // Act
        val read = { CatalogFile.read(text = text) }

        // Assert
        val error = assertFailsWith<CatalogFileException> { read() }
        assertFalse(actual = error.message.orEmpty().contains(other = "secret-marker"))
    }

    @Test
    fun `given an unknown format and a wrong shape, when it is read, then the format is refused first`() {
        // Arrange
        val text = """{"format":2,"components":5}"""

        // Act
        val read = { CatalogFile.read(text = text) }

        // Assert
        val error = assertFailsWith<CatalogFileException> { read() }
        assertTrue(actual = error.message.orEmpty().contains(other = "format 2 is not known"))
    }

    @Test
    fun `given a list of lists or of bytes, when the file is read, then the message says what a list holds`() {
        // Arrange
        val elements = listOf("""{"kind":"list"}""", """{"kind":"bytes"}""")

        // Act
        val unnamed = elements.filterNot { element ->
            val text = component(parameter = """{"name":"items","kind":"list","of":$element,"required":true}""")
            val error = runCatching { CatalogFile.read(text = text) }.exceptionOrNull()
            error is CatalogFileException && error.message.orEmpty().contains(other = "A list holds")
        }

        // Assert
        assertEquals(expected = emptyList(), actual = unnamed)
    }

    private fun component(parameter: String): String =
        """{"format":1,"version":"1","choices":[{"name":"A","entries":["b"]}],""" +
            """"components":[{"name":"Card","parameters":[$parameter]}]}"""

    private fun nested(levels: Int): String = "[".repeat(n = levels) + "]".repeat(n = levels)

    private companion object {
        const val DEEP_NESTING = 600_000
    }
}
