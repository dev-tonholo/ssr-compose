package dev.tonholo.ssrcompose.catalog.bundle.io

import dev.tonholo.ssrcompose.catalog.bundle.APP_PART_SUBJECT
import dev.tonholo.ssrcompose.catalog.bundle.CatalogFileException
import dev.tonholo.ssrcompose.catalog.bundle.SizeCap
import dev.tonholo.ssrcompose.catalog.bundle.partText
import dev.tonholo.ssrcompose.catalog.bundle.partWith
import dev.tonholo.ssrcompose.catalog.bundle.readPart
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CatalogPartStrictTest {
    @Test
    fun `given a format that is not a whole number, when the file is read, then each file is refused`() {
        // Arrange
        val formats = listOf(
            "1.5", "1.0", "1e0", "1E0", "+1", "01", "00", "+01", "4294967297", "true", "null", "[]", "{}",
        )

        // Act
        val notRefused = formats.filter { format ->
            // language=json
            val text = """{"format":$format,"part":"app","choices":[],"components":[]}"""
            runCatching { readPart(text = text) }.exceptionOrNull() !is CatalogFileException
        }

        // Assert
        assertEquals(expected = emptyList(), actual = notRefused)
    }

    @Test
    fun `given a flag that is not true or false, when the file is read, then each file is refused`() {
        // Arrange
        val flags = listOf(
            // language=json
            """"required":1""",
            // language=json
            """"required":0""",
            // language=json
            """"required":null""",
            // language=json
            """"required":"false"""",
            // language=json
            """"required":true,"nullable":"true"""",
            // language=json
            """"required":true,"nullable":null""",
        )

        // Act
        val notRefused = flags.filter { fields ->
            val text = partText(parameter = /* language=json */ """{"name":"title","kind":"text",$fields}""")
            runCatching { readPart(text = text) }.exceptionOrNull() !is CatalogFileException
        }

        // Assert
        assertEquals(expected = emptyList(), actual = notRefused)
    }

    @Test
    fun `given a field that does not belong to the kind, when the file is read, then each file is refused`() {
        // Arrange
        val parameters = listOf(
            // language=json
            """{"name":"onTap","kind":"callback","choice":"A","required":true}""",
            // language=json
            """{"name":"onTap","kind":"callback","of":{"kind":"text"},"required":true}""",
            // language=json
            """{"name":"onTap","kind":"callback","nullable":true,"required":true}""",
            // language=json
            """{"name":"onTap","kind":"callback","accepts":"any","required":true}""",
            // language=json
            """{"name":"content","kind":"slot","required":true}""",
            // language=json
            """{"name":"content","kind":"slot","accepts":"any","choice":"A","required":true}""",
            // language=json
            """{"name":"content","kind":"slot","accepts":"any","nullable":true,"required":true}""",
            // language=json
            """{"name":"title","kind":"text","arguments":[],"required":true}""",
            // language=json
            """{"name":"title","kind":"text","accepts":"any","required":true}""",
        )

        // Act
        val notRefused = parameters.filter { parameter ->
            runCatching { readPart(text = partText(parameter = parameter)) }
                .exceptionOrNull() !is CatalogFileException
        }

        // Assert
        assertEquals(expected = emptyList(), actual = notRefused)
    }

    @Test
    fun `given a kind with detail that does not belong to it, when the file is read, then each file is refused`() {
        // Arrange
        val kinds = listOf(
            // language=json
            """{"kind":"text","choice":"A"}""",
            // language=json
            """{"kind":"text","of":{"kind":"text"}}""",
            // language=json
            """{"kind":"choice","choice":"A","of":{"kind":"text"}}""",
            // language=json
            """{"kind":"list","choice":"A","of":{"kind":"text"}}""",
        )
        val parameters = kinds.flatMap { kind ->
            listOf(
                // language=json
                """{"name":"p",${kind.removeSurrounding(prefix = "{", suffix = "}")},"required":true}""",
                // language=json
                """{"name":"onTap","kind":"callback","arguments":[$kind],"required":true}""",
                // language=json
                """{"name":"content","kind":"slot","accepts":"any","arguments":[$kind],"required":true}""",
            )
        } + /* language=json */ """{"name":"items","kind":"list","of":{"kind":"text","choice":"A"},"required":true}"""

        // Act
        val notRefused = parameters.filter { parameter ->
            runCatching { readPart(text = partText(parameter = parameter)) }
                .exceptionOrNull() !is CatalogFileException
        }

        // Assert
        assertEquals(expected = emptyList(), actual = notRefused)
    }

    @Test
    fun `given a slot that accepts an unknown value, when the file is read, then it is refused`() {
        // Arrange
        val text = partText(
            // language=json
            parameter = """{"name":"content","kind":"slot","accepts":"server","required":true}""",
        )

        // Act
        val read = { readPart(text = text) }

        // Assert
        assertFailsWith<CatalogFileException> { read() }
    }

    @Test
    fun `given an unknown key at each level, when the file is read, then each file is refused`() {
        // Arrange
        // language=json
        val extra = ""","extra":1"""
        // language=json
        val listArgument = """{"kind":"list","of":{"kind":"text"}$extra}"""
        // language=json
        val choiceElement = """{"kind":"choice","choice":"A"$extra}"""
        val files = listOf(
            // language=json
            """{"format":1,"part":"app","choices":[],"components":[]$extra}""",
            // language=json
            """{"format":1,"part":"app","choices":[{"name":"A","entries":["b"]$extra}],"components":[]}""",
            // language=json
            """{"format":1,"part":"app","choices":[],"components":[{"name":"Card","parameters":[]$extra}]}""",
            partText(parameter = /* language=json */ """{"name":"title","kind":"text","required":true$extra}"""),
            partText(parameter = /* language=json */ """{"name":"onTap","kind":"callback","required":true$extra}"""),
            partText(
                // language=json
                parameter = """{"name":"content","kind":"slot","accepts":"any","required":true$extra}""",
            ),
            partText(
                // language=json
                parameter = """{"name":"on","kind":"callback","arguments":[$listArgument],"required":true}""",
            ),
            partText(
                // language=json
                parameter = """{"name":"items","kind":"list","of":$choiceElement,"required":true}""",
            ),
        )

        // Act
        val notRefused = files.filter { file ->
            runCatching { readPart(text = file) }.exceptionOrNull() !is CatalogFileException
        }

        // Assert
        assertEquals(expected = emptyList(), actual = notRefused)
    }

    @Test
    fun `given an unknown key, when the file is refused, then the message does not repeat the key`() {
        // Arrange
        // language=json
        val text = """{"format":1,"part":"app","choices":[],"components":[],"secret-marker":1}"""

        // Act
        val read = { readPart(text = text) }

        // Assert
        val error = assertFailsWith<CatalogFileException> { read() }
        assertFalse(actual = error.message.orEmpty().contains(other = "secret-marker"))
    }

    @Test
    fun `given a text that is not JSON, when the file is refused, then the message does not repeat the text`() {
        // Arrange
        val text = "secret-marker is not a catalog"

        // Act
        val read = { readPart(text = text) }

        // Assert
        val error = assertFailsWith<CatalogFileException> { read() }
        assertFalse(actual = error.message.orEmpty().contains(other = "secret-marker"))
    }

    @Test
    fun `given a kind that is not known, when the file is refused, then the message does not repeat the kind`() {
        // Arrange
        val text = partText(
            // language=json
            parameter = """{"name":"title","kind":"secret-marker","required":true}""",
        )

        // Act
        val read = { readPart(text = text) }

        // Assert
        val error = assertFailsWith<CatalogFileException> { read() }
        assertFalse(actual = error.message.orEmpty().contains(other = "secret-marker"))
    }

    @Test
    fun `given another format and a wrong shape, when it is read, then the format is refused first`() {
        // Arrange
        val formats = listOf("0", "-1", "2")

        // Act
        val notNamed = formats.filterNot { format ->
            val error = runCatching {
                readPart(
                    // language=json
                    text = """{"format":$format,"components":5}""",
                )
            }.exceptionOrNull()
            error is CatalogFileException && error.message.orEmpty().contains(other = "format $format is not known")
        }

        // Assert
        assertEquals(expected = emptyList(), actual = notNamed)
    }

    @Test
    fun `given an object with the same key twice, when the file is read, then each file is refused`() {
        // Arrange
        // language=json
        val repeatedKind = """{"kind":"text","kind":"flag"}"""
        val files = listOf(
            // language=json
            """{"format":1,"part":"app","part":"home","choices":[],"components":[]}""",
            // language=json
            """{"format":1,"format":1,"part":"app","choices":[],"components":[]}""",
            // language=json
            """{"format":1,"part":"app","choices":[{"name":"A","name":"B","entries":["b"]}],"components":[]}""",
            // language=json
            """{"format":1,"part":"app","choices":[],"components":[{"name":"C","name":"D","parameters":[]}]}""",
            partText(
                // language=json
                parameter = """{"name":"title","name":"text","kind":"text","required":true}""",
            ),
            partText(
                // language=json
                parameter = """{"name":"i","kind":"list","of":{"kind":"text","kind":"whole"},"required":true}""",
            ),
            partText(
                // language=json
                parameter = """{"name":"on","kind":"callback","arguments":[$repeatedKind],"required":true}""",
            ),
        )

        // Act
        val notRefused = files.filter { file ->
            runCatching { readPart(text = file) }.exceptionOrNull() !is CatalogFileException
        }

        // Assert
        assertEquals(expected = emptyList(), actual = notRefused)
    }

    @Test
    fun `given a comma after the last member, when the file is read, then each file is refused`() {
        // Arrange
        val files = listOf(
            // language=json
            """{"format":1,"part":"app","choices":[],"components":[],}""",
            // language=json
            """{"format":1,"part":"app","choices":[],"components":[{"name":"Card","parameters":[],}]}""",
            // language=json
            """{"format":1,"part":"app","choices":[{"name":"A","entries":["b",]}],"components":[]}""",
        )

        // Act
        val notRefused = files.filter { file ->
            runCatching { readPart(text = file) }.exceptionOrNull() !is CatalogFileException
        }

        // Assert
        assertEquals(expected = emptyList(), actual = notRefused)
    }

    @Test
    fun `given a comment, when the file is read, then each file is refused`() {
        // Arrange
        val files = listOf(
            "// the part\n" + /* language=json */ """{"format":1,"part":"app","choices":[],"components":[]}""",
            // language=json
            """{"format":1,"part":"app","choices":[],"components":[]} // done""",
            // language=json
            """{"format":1,/* the part */"part":"app","choices":[],"components":[]}""",
        )

        // Act
        val notRefused = files.filter { file ->
            runCatching { readPart(text = file) }.exceptionOrNull() !is CatalogFileException
        }

        // Assert
        assertEquals(expected = emptyList(), actual = notRefused)
    }

    @Test
    fun `given a list of lists or of bytes, when the file is read, then the message says what a list holds`() {
        // Arrange
        val elements = listOf(/* language=json */ """{"kind":"list"}""", /* language=json */ """{"kind":"bytes"}""")

        // Act
        val unnamed = elements.filterNot { element ->
            // language=json
            val parameter = """{"name":"items","kind":"list","of":$element,"required":true}"""
            val text = partText(parameter = parameter)
            val error = runCatching { readPart(text = text) }.exceptionOrNull()
            error is CatalogFileException && error.message.orEmpty().contains(other = "A list holds")
        }

        // Assert
        assertEquals(expected = emptyList(), actual = unnamed)
    }

    @Test
    fun `given members with no comma between them, when the file is read, then it is refused`() {
        // Arrange
        val canonical = CatalogPartWriter.write(part = partWith()).decodeToString()
        val missingComma = canonical.replace(oldValue = "\"format\": 1,\n", newValue = "\"format\": 1\n")

        // Act
        val read = { readPart(text = missingComma) }

        // Assert
        val error = assertFailsWith<CatalogFileException> { read() }
        assertTrue(actual = error.message.orEmpty().contains(other = APP_PART_SUBJECT))
    }

    @Test
    fun `given a file with extra whitespace, when the file is read, then it is refused`() {
        // Arrange
        val canonical = CatalogPartWriter.write(part = partWith()).decodeToString()
        val extraWhitespace = canonical.replace(oldValue = "\"format\": 1,", newValue = "\"format\":  1,")

        // Act
        val read = { readPart(text = extraWhitespace) }

        // Assert
        val error = assertFailsWith<CatalogFileException> { read() }
        assertTrue(actual = error.message.orEmpty().contains(other = APP_PART_SUBJECT))
    }

    @Test
    fun `given keys in another order, when the file is read, then it is refused`() {
        // Arrange
        val wrongOrder = "{\n  \"part\": \"app\",\n  \"format\": 1,\n  \"choices\": [],\n  \"components\": []\n}\n"

        // Act
        val read = { readPart(text = wrongOrder) }

        // Assert
        val error = assertFailsWith<CatalogFileException> { read() }
        assertTrue(actual = error.message.orEmpty().contains(other = APP_PART_SUBJECT))
    }
}
