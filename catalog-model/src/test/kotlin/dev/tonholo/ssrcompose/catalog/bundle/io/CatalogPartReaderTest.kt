package dev.tonholo.ssrcompose.catalog.bundle.io

import dev.tonholo.ssrcompose.catalog.bundle.APP_PART_SUBJECT
import dev.tonholo.ssrcompose.catalog.bundle.CatalogFileException
import dev.tonholo.ssrcompose.catalog.bundle.DEFAULT_CAP_BYTES
import dev.tonholo.ssrcompose.catalog.bundle.SizeCap
import dev.tonholo.ssrcompose.catalog.bundle.partAtCapPair
import dev.tonholo.ssrcompose.catalog.bundle.partText
import dev.tonholo.ssrcompose.catalog.bundle.partWith
import dev.tonholo.ssrcompose.catalog.bundle.readPart
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.core.data.ValueKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CatalogPartReaderTest {
    @Test
    fun `given a part of exactly 16 MiB, when it is read with the default cap, then it is accepted`() {
        // Arrange
        val (atCap, _) = partAtCapPair(name = "app")
        val bytes = CatalogPartWriter.write(part = atCap)

        // Act
        val actual = CatalogPartReader.read(bytes = bytes, sizeCap = SizeCap.DEFAULT, subject = APP_PART_SUBJECT)

        // Assert
        assertEquals(expected = atCap, actual = actual)
    }

    @Test
    fun `given a part one byte over 16 MiB, when it is read with the default cap, then it is refused`() {
        // Arrange
        val text = " ".repeat(n = DEFAULT_CAP_BYTES + 1)

        // Act
        val read = { readPart(text = text) }

        // Assert
        val error = assertFailsWith<CatalogFileException> { read() }
        assertTrue(actual = error.message.orEmpty().contains(other = "larger than $DEFAULT_CAP_BYTES bytes"))
    }

    @Test
    fun `given 20000 components and a component with 2000 parameters, when written and read, then all are read`() {
        // Arrange
        val wide = ComponentDescriptor(name = "Wide", parameters = textParameters(count = MANY_PARAMETERS))
        val others = List(size = MANY_COMPONENTS) { index ->
            ComponentDescriptor(name = "C$index", parameters = emptyList())
        }
        val bytes = CatalogPartWriter.write(part = partWith(components = others + wide))

        // Act
        val actual = CatalogPartReader.read(bytes = bytes, sizeCap = SizeCap.DEFAULT, subject = APP_PART_SUBJECT)

        // Assert
        val actualWide = actual.components.single { it.name == "Wide" }
        assertEquals(expected = MANY_COMPONENTS + 1, actual = actual.components.size)
        assertEquals(expected = MANY_PARAMETERS, actual = actualWide.parameters.size)
    }

    @Test
    fun `given values of the wrong type, when the file is read, then each file is refused`() {
        // Arrange
        // language=json
        val parameter = """{"name":"title","kind":"text","required":true}"""
        val wrongFiles = listOf(
            // language=json
            """[]""",
            // language=json
            """{"format":1,"part":1,"choices":[],"components":[]}""",
            // language=json
            """{"format":1,"part":"app","choices":{},"components":[]}""",
            // language=json
            """{"format":1,"part":"app","choices":[],"components":{}}""",
            // language=json
            """{"format":1,"part":"app","choices":[],"components":[1]}""",
            // language=json
            """{"format":1,"part":"app","choices":[{"name":"A","entries":"x"}],"components":[]}""",
            partText(parameter = /* language=json */ """{"name":5,"kind":"text","required":true}"""),
            partText(parameter = /* language=json */ """{"name":"title","kind":5,"required":true}"""),
            partText(
                // language=json
                parameter = """{"name":"title","kind":"text","required":true,"nullable":"yes"}""",
            ),
            partText(
                // language=json
                parameter = """{"name":"onTap","kind":"callback","arguments":"text","required":true}""",
            ),
            partText(
                // language=json
                parameter = """{"name":"onTap","kind":"callback","arguments":[5],"required":true}""",
            ),
            partText(
                // language=json
                parameter = """{"name":"onTap","kind":"callback","arguments":["text"],"required":true}""",
            ),
            partText(parameter = /* language=json */ """{"name":"items","kind":"list","of":"text","required":true}"""),
            partText(
                // language=json
                parameter = """{"name":"content","kind":"slot","accepts":1,"required":true}""",
            ),
            partText(parameter = parameter.replace(oldValue = "text", newValue = "list")),
        )

        // Act
        val notRefused = wrongFiles.filter { file ->
            runCatching { readPart(text = file) }.exceptionOrNull() !is CatalogFileException
        }

        // Assert
        assertEquals(expected = emptyList(), actual = notRefused)
    }

    private fun textParameters(count: Int): List<Parameter> =
        List(size = count) { index -> Parameter.Value(name = "p$index", kind = ValueKind.Text, required = true) }

    private companion object {
        const val MANY_COMPONENTS = 20_000
        const val MANY_PARAMETERS = 2_000
    }
}
