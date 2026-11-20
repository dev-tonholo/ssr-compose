package dev.tonholo.ssrcompose.catalog.index

import dev.tonholo.ssrcompose.catalog.bundle.CatalogFileException
import dev.tonholo.ssrcompose.catalog.bundle.SizeCap
import dev.tonholo.ssrcompose.catalog.model.Catalog
import dev.tonholo.ssrcompose.catalog.model.CatalogPart
import dev.tonholo.ssrcompose.catalog.model.ChoiceList
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.catalog.model.PartName
import dev.tonholo.ssrcompose.catalog.model.SlotAccepts
import dev.tonholo.ssrcompose.catalog.testing.HandEncodedChoice
import dev.tonholo.ssrcompose.catalog.testing.HandEncodedComponent
import dev.tonholo.ssrcompose.catalog.testing.HandEncodedIndex
import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
import dev.tonholo.ssrcompose.core.data.ValueKind
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CatalogIndexTest {
    private val written = CatalogIndexWriter.write(catalog = catalogOf(everyKindComponent(), EMPTY_COMPONENT))

    @Test
    fun `given one saved catalog, when it is written twice, then the bytes are the same`() {
        // Arrange
        val first = twoPartCatalog()
        val second = twoPartCatalog()

        // Act
        val firstBytes = CatalogIndexWriter.write(catalog = first)
        val secondBytes = CatalogIndexWriter.write(catalog = second)

        // Assert
        assertContentEquals(expected = firstBytes, actual = secondBytes)
    }

    @Test
    fun `given parts in another order, when the catalog is written, then the bytes are the same`() {
        // Arrange
        val catalog = twoPartCatalog()
        val reordered = catalog.copy(parts = catalog.parts.reversed())

        // Act
        val firstBytes = CatalogIndexWriter.write(catalog = catalog)
        val secondBytes = CatalogIndexWriter.write(catalog = reordered)

        // Assert
        assertContentEquals(expected = firstBytes, actual = secondBytes)
    }

    @Test
    fun `given one changed component, when the catalog is written, then the bytes differ`() {
        // Arrange
        val changed = catalogOf(everyKindComponent().copy(parameters = emptyList()), EMPTY_COMPONENT)

        // Act
        val changedBytes = CatalogIndexWriter.write(catalog = changed)

        // Assert
        assertFalse(actual = written.contentEquals(changedBytes))
    }

    @Test
    fun `given the smallest catalog, when it is written, then the index holds only the catalog and no path or time`() {
        // Arrange
        val catalog = Catalog(
            version = CatalogVersion(value = "1"),
            parts = listOf(
                CatalogPart(
                    name = PartName(value = "only"),
                    choices = emptyList(),
                    components = listOf(ComponentDescriptor(name = "A", parameters = emptyList())),
                ),
            ),
        )

        // Act
        val bytes = CatalogIndexWriter.write(catalog = catalog)

        // Assert
        assertContentEquals(expected = HAND_ENCODED_SMALLEST_INDEX, actual = bytes)
    }

    @Test
    fun `given a written catalog, when it is read, then it holds the catalog version`() {
        // Arrange
        val bytes = written

        // Act
        val index = CatalogIndexReader.read(bytes = bytes)

        // Assert
        assertEquals(expected = CatalogVersion(value = VERSION), actual = index.version)
    }

    @Test
    fun `given a written catalog, when it is read, then it holds every choice list sorted by name`() {
        // Arrange
        val choices = listOf(ChoiceList(name = "Zone", entries = listOf("North")), TONE)
        val catalog = catalogOf(EMPTY_COMPONENT, choices = choices)

        // Act
        val index = CatalogIndexReader.read(bytes = CatalogIndexWriter.write(catalog = catalog))

        // Assert
        assertEquals(expected = listOf(TONE, choices.first()), actual = index.choices)
    }

    @Test
    fun `given one choice list in two parts, when the catalog is read back, then it holds that list once`() {
        // Arrange
        val catalog = twoPartCatalog()

        // Act
        val index = CatalogIndexReader.read(bytes = CatalogIndexWriter.write(catalog = catalog))

        // Assert
        assertEquals(expected = listOf(TONE), actual = index.choices)
    }

    @Test
    fun `given a written catalog, when it is read, then it names every component in sorted order`() {
        // Arrange
        val bytes = written

        // Act
        val index = CatalogIndexReader.read(bytes = bytes)

        // Assert
        assertEquals(expected = listOf(EMPTY_COMPONENT_NAME, EVERY_KIND_NAME), actual = index.componentNames)
    }

    @Test
    fun `given every kind of parameter, when it is read back, then nothing is lost`() {
        // Arrange
        val bytes = written

        // Act
        val component = CatalogIndexReader.read(bytes = bytes).component(name = EVERY_KIND_NAME)

        // Assert
        assertEquals(expected = everyKindComponent(), actual = component)
    }

    @Test
    fun `given a slot with 256 arguments, when it is read back, then every argument is kept`() {
        // Arrange
        val slot = Parameter.Slot(
            name = "content",
            arguments = List(size = ABOVE_UNSIGNED_BYTE) { ValueKind.Text },
            required = true,
        )
        val component = ComponentDescriptor(name = EVERY_KIND_NAME, parameters = listOf(slot))

        // Act
        val index = CatalogIndexReader.read(bytes = CatalogIndexWriter.write(catalog = catalogOf(component)))

        // Assert
        assertEquals(expected = component, actual = index.component(name = EVERY_KIND_NAME))
    }

    @Test
    fun `given a name the catalog does not hold, when it is looked up, then nothing is found`() {
        // Arrange
        val index = CatalogIndexReader.read(bytes = written)

        // Act
        val component = index.component(name = "NotInTheCatalog")

        // Assert
        assertNull(actual = component)
    }

    @Test
    fun `given bytes of another file, when they are read, then a clear error says it is not an index`() {
        // Arrange
        val bytes = "this file is not an index".encodeToByteArray()

        // Act
        val failure = assertFailsWith<CatalogFileException> { CatalogIndexReader.read(bytes = bytes) }

        // Assert
        assertTrue(actual = failure.message.orEmpty().contains(NOT_VALID), message = failure.message)
    }

    @Test
    fun `given an index cut short at any point, when it is read, then it is refused`() {
        // Arrange
        val cuts = written.indices.map { size -> written.copyOf(newSize = size) }

        // Act
        val failures = cuts.map { cut -> runCatching { readEverything(bytes = cut) }.exceptionOrNull() }

        // Assert
        val notRefused = failures.withIndex().filterNot { (_, failure) -> failure is CatalogFileException }
        assertEquals(expected = emptyList(), actual = notRefused.map { (size, failure) -> size to failure })
    }

    @Test
    fun `given one damaged byte at any point, when the index is read, then only a clear error can fail it`() {
        // Arrange
        val damaged = written.indices.flatMap { position ->
            DAMAGED_VALUES.map { value -> written.copyOf().also { bytes -> bytes[position] = value } }
        }

        // Act
        val failures = damaged.mapNotNull { bytes -> runCatching { readEverything(bytes = bytes) }.exceptionOrNull() }

        // Assert
        assertEquals(expected = emptyList(), actual = failures.filterNot { it is CatalogFileException })
    }

    @Test
    fun `given bytes after the last component, when the index is read, then it is refused`() {
        // Arrange
        val longer = written.copyOf(newSize = written.size + 1)

        // Act
        val failure = runCatching { CatalogIndexReader.read(bytes = longer) }.exceptionOrNull()

        // Assert
        assertTrue(actual = failure is CatalogFileException, message = failure.toString())
    }

    @Test
    fun `given a string count far above the file size, when the index is read, then it is refused`() {
        // Arrange
        val bytes = HAND_ENCODED_SMALLEST_INDEX.copyOf()
        ByteBuffer.wrap(bytes).putInt(SMALLEST_INDEX_STRING_COUNT_FIELD, Int.MAX_VALUE)

        // Act
        val failure = runCatching { CatalogIndexReader.read(bytes = bytes) }.exceptionOrNull()

        // Assert
        assertTrue(actual = failure is CatalogFileException, message = failure.toString())
    }

    @Test
    fun `given many references to one choice list, when the component is read, then they share one decoded name`() {
        // Arrange
        val bytes = HandEncodedIndex.encode(
            strings = REPEATING_STRINGS.map { it.encodeToByteArray() },
            choices = listOf(HandEncodedChoice(nameId = REPEATED_LIST_ID, entryIds = listOf(REPEATED_ENTRY_ID))),
            components = listOf(HandEncodedComponent(nameId = REPEATING_COMPONENT_ID, bodyOffset = 0)),
            bodies = HandEncodedIndex.bodyOf(
                HandEncodedIndex.slot(
                    nameId = REPEATING_SLOT_ID,
                    kinds = List(size = REPEATED_REFERENCES) { HandEncodedIndex.choiceKind(REPEATED_LIST_ID) },
                ),
            ),
        )

        // Act
        val component = CatalogIndexReader.read(bytes = bytes).component(name = REPEATING_COMPONENT)

        // Assert
        val slot = component?.parameters?.single() as Parameter.Slot
        val names = slot.arguments.map { argument -> (argument as ValueKind.Choice).listName }
        assertEquals(expected = REPEATED_REFERENCES, actual = names.size)
        assertTrue(actual = names.all { name -> name === names.first() }, message = "The name was decoded again")
    }

    @Test
    fun `given a stream over the size cap that is not an index, when it is read, then the error states the cap`() {
        // Arrange
        val notAnIndex = ByteArray(size = OVER_CAP_BYTES)
        val sizeCap = SizeCap(bytes = OVER_CAP_BYTES - 1)

        // Act
        val failure = assertFailsWith<CatalogFileException> {
            CatalogIndexReader.read(stream = ByteArrayInputStream(notAnIndex), sizeCap = sizeCap)
        }

        // Assert
        assertTrue(actual = failure.message.orEmpty().contains(LARGER_THAN_CAP), message = failure.message)
    }

    @Test
    fun `given an index over the size cap, when it is written, then the error states the cap`() {
        // Arrange
        val sizeCap = SizeCap(bytes = written.size - 1)

        // Act
        val failure = assertFailsWith<CatalogFileException> {
            CatalogIndexWriter.write(catalog = catalogOf(everyKindComponent(), EMPTY_COMPONENT), sizeCap = sizeCap)
        }

        // Assert
        assertTrue(actual = failure.message.orEmpty().contains(LARGER_THAN_CAP), message = failure.message)
    }

    @Test
    fun `given an index exactly at the size cap, when it is written, then the reader opens it under that cap`() {
        // Arrange
        val sizeCap = SizeCap(bytes = written.size)
        val catalog = catalogOf(everyKindComponent(), EMPTY_COMPONENT)

        // Act
        val bytes = CatalogIndexWriter.write(catalog = catalog, sizeCap = sizeCap)

        // Assert
        val index = CatalogIndexReader.read(stream = ByteArrayInputStream(bytes), sizeCap = sizeCap)
        assertEquals(expected = listOf(EMPTY_COMPONENT_NAME, EVERY_KIND_NAME), actual = index.componentNames)
    }

    @Test
    fun `given a stream within the size cap, when it is read, then the index opens`() {
        // Arrange
        val stream = ByteArrayInputStream(written)

        // Act
        val index = CatalogIndexReader.read(stream = stream, sizeCap = SizeCap(bytes = written.size))

        // Assert
        assertEquals(expected = listOf(EMPTY_COMPONENT_NAME, EVERY_KIND_NAME), actual = index.componentNames)
    }

    private fun readEverything(bytes: ByteArray) {
        val index = CatalogIndexReader.read(bytes = bytes)
        index.componentNames.forEach { name -> index.component(name = name) }
    }

    private fun twoPartCatalog() = Catalog(
        version = CatalogVersion(value = VERSION),
        parts = listOf(
            CatalogPart(
                name = PartName(value = "first"),
                choices = listOf(TONE),
                components = listOf(everyKindComponent()),
            ),
            CatalogPart(
                name = PartName(value = "second"),
                choices = listOf(TONE),
                components = listOf(EMPTY_COMPONENT),
            ),
        ),
    )

    private fun catalogOf(
        vararg components: ComponentDescriptor,
        choices: List<ChoiceList> = listOf(TONE),
    ) = Catalog(
        version = CatalogVersion(value = VERSION),
        parts = listOf(
            CatalogPart(name = PartName(value = "only"), choices = choices, components = components.toList()),
        ),
    )

    private fun everyKindComponent() = ComponentDescriptor(
        name = EVERY_KIND_NAME,
        parameters = listOf(
            Parameter.Value(name = "text", kind = ValueKind.Text, required = true),
            Parameter.Value(name = "whole", kind = ValueKind.Whole, required = false, nullable = true),
            Parameter.Value(name = "decimal", kind = ValueKind.Decimal, required = true),
            Parameter.Value(name = "flag", kind = ValueKind.Flag, required = false),
            Parameter.Value(name = "choice", kind = ValueKind.Choice(listName = TONE.name), required = true),
            Parameter.Value(
                name = "choices",
                kind = ValueKind.ListOf(element = ValueKind.Choice(listName = TONE.name)),
                required = true,
            ),
            Parameter.Callback(name = "onBytes", arguments = listOf(ValueKind.Bytes), required = false),
            Parameter.Callback(name = "onNothing", required = true),
            Parameter.Slot(
                name = "content",
                accepts = SlotAccepts.Consumer,
                arguments = listOf(ValueKind.Text, ValueKind.Whole),
                required = true,
            ),
            Parameter.Slot(name = "footer", accepts = SlotAccepts.Producer, required = false),
        ),
    )

    private companion object {
        const val VERSION = "2.5-beta"
        const val EVERY_KIND_NAME = "EveryKind"
        const val EMPTY_COMPONENT_NAME = "Divider"
        const val NOT_VALID = "The catalog index is not valid"
        const val LARGER_THAN_CAP = "is larger than"
        const val OVER_CAP_BYTES = 64
        const val ABOVE_UNSIGNED_BYTE = 256
        const val SMALLEST_INDEX_STRING_COUNT_FIELD = 9
        const val REPEATING_COMPONENT_ID = 1
        const val REPEATING_SLOT_ID = 2
        const val REPEATED_LIST_ID = 3
        const val REPEATED_ENTRY_ID = 4
        const val REPEATED_REFERENCES = 2_000
        const val REPEATING_COMPONENT = "Repeating"
        val REPEATING_STRINGS = listOf("1", REPEATING_COMPONENT, "content", "Tone", "Warm")
        val TONE = ChoiceList(name = "Tone", entries = listOf("First", "Second"))
        val EMPTY_COMPONENT = ComponentDescriptor(name = EMPTY_COMPONENT_NAME, parameters = emptyList())
        val DAMAGED_VALUES = listOf<Byte>(-1, Byte.MIN_VALUE, Byte.MAX_VALUE)
        val HAND_ENCODED_SMALLEST_INDEX = byteArrayOf(
            0x53, 0x53, 0x52, 0x49,
            0x01,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x02,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x01,
            0x00, 0x00, 0x00, 0x02,
            0x31, 0x41,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x01,
            0x00, 0x00, 0x00, 0x01,
            0x00, 0x00, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x04,
            0x00, 0x00, 0x00, 0x00,
        )
    }
}
