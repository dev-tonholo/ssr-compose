package dev.tonholo.ssrcompose.catalog.testing.indexformat

import dev.tonholo.ssrcompose.catalog.model.Catalog
import dev.tonholo.ssrcompose.catalog.model.CatalogPart
import dev.tonholo.ssrcompose.catalog.model.ChoiceList
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.catalog.model.PartName
import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
import dev.tonholo.ssrcompose.core.data.ValueKind
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BinaryIndexFormatTest {
    private val written = BinaryIndexFormat.write(catalog = catalog())

    @Test
    fun `given only the magic and version bytes, when the index is opened, then it is refused`() {
        // Arrange
        val headerOnly = written.copyOf(newSize = MAGIC_AND_VERSION_BYTES)

        // Act
        val failure = runCatching { BinaryIndexFormat.open(bytes = headerOnly) }.exceptionOrNull()

        // Assert
        assertIs<IllegalArgumentException>(value = failure)
    }

    @Test
    fun `given an index cut short at any point, when it is opened and read, then it is refused`() {
        // Arrange
        val cuts = written.indices.map { size -> written.copyOf(newSize = size) }

        // Act
        val failures = cuts.map { cut -> runCatching { openAndReadAll(bytes = cut) }.exceptionOrNull() }

        // Assert
        val notRefused = failures.withIndex().filterNot { (_, failure) -> failure is IllegalArgumentException }
        assertEquals(expected = emptyList(), actual = notRefused.map { (size, failure) -> size to failure })
    }

    @Test
    fun `given one damaged byte at any point, when the index is opened and read, then only a refusal can fail it`() {
        // Arrange
        val damaged = written.indices.flatMap { position ->
            DAMAGED_VALUES.map { value -> written.copyOf().also { bytes -> bytes[position] = value } }
        }

        // Act
        val failures = damaged.mapNotNull { bytes -> runCatching { openAndReadAll(bytes = bytes) }.exceptionOrNull() }

        // Assert
        assertEquals(expected = emptyList(), actual = failures.filterNot { it is IllegalArgumentException })
    }

    @Test
    fun `given bytes after the last component, when the index is opened, then it is refused`() {
        // Arrange
        val longer = written.copyOf(newSize = written.size + 1)

        // Act
        val failure = runCatching { BinaryIndexFormat.open(bytes = longer) }.exceptionOrNull()

        // Assert
        assertIs<IllegalArgumentException>(value = failure)
    }

    @Test
    fun `given a component offset past the last body, when the index is opened, then it is refused`() {
        // Arrange
        val bytes = BinaryIndexFormat.write(catalog = catalogOf(components = listOf(EMPTY_COMPONENT)))
        val offsetField = ByteBuffer.wrap(bytes).position(bytes.size - OFFSET_FIELD_FROM_END)
        offsetField.putInt(EMPTY_BODY_BYTES)

        // Act
        val failure = runCatching { BinaryIndexFormat.open(bytes = bytes) }.exceptionOrNull()

        // Assert
        assertIs<IllegalArgumentException>(value = failure)
    }

    private fun openAndReadAll(bytes: ByteArray) {
        val index = BinaryIndexFormat.open(bytes = bytes)
        index.componentNames.forEach { name -> index.component(name = name) }
    }

    private fun catalog() = catalogOf(
        components = listOf(
            ComponentDescriptor(
                name = "Card",
                parameters = listOf(
                    Parameter.Value(name = "title", kind = ValueKind.Text, required = true),
                    Parameter.Value(
                        name = "tones",
                        kind = ValueKind.ListOf(element = ValueKind.Choice(listName = CHOICE_LIST)),
                        required = false,
                        nullable = true,
                    ),
                    Parameter.Callback(name = "onTap", arguments = listOf(ValueKind.Text), required = true),
                    Parameter.Slot(name = "content", arguments = listOf(ValueKind.Whole), required = false),
                ),
            ),
            EMPTY_COMPONENT,
        ),
    )

    private fun catalogOf(components: List<ComponentDescriptor>) = Catalog(
        version = CatalogVersion(value = "1"),
        parts = listOf(
            CatalogPart(
                name = PartName(value = "only"),
                choices = listOf(ChoiceList(name = CHOICE_LIST, entries = listOf("First", "Second"))),
                components = components,
            ),
        ),
    )

    private companion object {
        const val MAGIC_AND_VERSION_BYTES = 5
        const val EMPTY_BODY_BYTES = 4
        const val OFFSET_FIELD_FROM_END = 12
        const val CHOICE_LIST = "Tone"
        val EMPTY_COMPONENT = ComponentDescriptor(name = "Divider", parameters = emptyList())
        val DAMAGED_VALUES = listOf<Byte>(-1, Byte.MIN_VALUE, Byte.MAX_VALUE)
    }
}
