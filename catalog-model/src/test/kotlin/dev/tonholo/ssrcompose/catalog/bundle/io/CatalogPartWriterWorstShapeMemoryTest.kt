package dev.tonholo.ssrcompose.catalog.bundle.io

import dev.tonholo.ssrcompose.catalog.bundle.DEFAULT_CAP_BYTES
import dev.tonholo.ssrcompose.catalog.bundle.partWith
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.core.data.ValueKind
import kotlin.test.Test
import kotlin.test.assertEquals

class CatalogPartWriterWorstShapeMemoryTest {
    @Test
    fun `given a part that is written at the cap, when it is written, then it is written without running out`() {
        // Arrange
        val emptySize = writtenSize(components = emptyList())
        val oneSize = writtenSize(components = listOf(componentNamed(index = 0)))
        val twoSize = writtenSize(components = listOf(componentNamed(index = 0), componentNamed(index = 1)))
        val perComponent = twoSize - oneSize
        val tailOverhead = writtenSize(components = listOf(tailComponent(extraChars = 0))) - emptySize
        val count = (DEFAULT_CAP_BYTES - emptySize - tailOverhead) / perComponent
        val remainder = DEFAULT_CAP_BYTES - (emptySize + count * perComponent + tailOverhead)
        val part = partWith(components = componentsNamed(count = count) + tailComponent(extraChars = remainder))

        // Act
        val actual = CatalogPartWriter.write(part = part)

        // Assert
        assertEquals(expected = DEFAULT_CAP_BYTES, actual = actual.size)
    }

    private fun writtenSize(components: List<ComponentDescriptor>): Int =
        CatalogPartWriter.write(part = partWith(components = components)).size

    private fun componentsNamed(count: Int): List<ComponentDescriptor> =
        List(size = count) { index -> componentNamed(index = index) }

    private fun componentNamed(index: Int): ComponentDescriptor = ComponentDescriptor(
        name = "C" + index.toString().padStart(length = NAME_DIGITS, padChar = '0'),
        parameters = listOf(Parameter.Value(name = "p", kind = ValueKind.Text, required = true)),
    )

    private fun tailComponent(extraChars: Int): ComponentDescriptor {
        var remaining = extraChars
        val nameExtra = minOf(remaining, MAX_EXTRA_CHARS)
        remaining -= nameExtra
        val firstExtra = minOf(remaining, MAX_EXTRA_CHARS)
        remaining -= firstExtra
        return ComponentDescriptor(
            name = "Z" + "a".repeat(n = nameExtra),
            parameters = listOf(
                Parameter.Value(name = "p" + "a".repeat(n = firstExtra), kind = ValueKind.Text, required = true),
                Parameter.Value(name = "q" + "a".repeat(n = remaining), kind = ValueKind.Text, required = true),
            ),
        )
    }

    private companion object {
        const val NAME_DIGITS = 7
        const val MAX_EXTRA_CHARS = 60
    }
}
