package dev.tonholo.ssrcompose.catalog.testing.indexformat

import dev.tonholo.ssrcompose.catalog.model.Catalog
import dev.tonholo.ssrcompose.catalog.model.CatalogPart
import dev.tonholo.ssrcompose.catalog.model.ChoiceList
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.catalog.model.PartName
import dev.tonholo.ssrcompose.catalog.model.SlotAccepts
import dev.tonholo.ssrcompose.catalog.testing.ParameterPalette
import dev.tonholo.ssrcompose.catalog.testing.SyntheticCatalog
import dev.tonholo.ssrcompose.catalog.testing.SyntheticCatalogShape
import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
import dev.tonholo.ssrcompose.core.data.ValueKind
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

class CandidateIndexFormatTest {
    private val catalog = SyntheticCatalog.generate(
        shape = SyntheticCatalogShape(
            components = COMPONENTS,
            parts = PARTS,
            seed = SEED,
            palette = ParameterPalette.Full,
            version = VERSION,
        ),
    )

    @Test
    fun `given a catalog, when each format writes and opens its index, then every component reads back equal`() {
        CandidateIndexFormats.all.forEach { format ->
            // Arrange
            val expected = catalog.parts.flatMap { it.components }.sortedBy { it.name }

            // Act
            val index = format.open(bytes = format.write(catalog = catalog))
            val actual = index.componentNames.map { name -> index.component(name = name) }

            // Assert
            assertEquals(expected = expected, actual = actual, message = format.name)
        }
    }

    @Test
    fun `given a catalog, when each format opens its index, then the version and choice lists read back equal`() {
        CandidateIndexFormats.all.forEach { format ->
            // Arrange
            val expected = catalog.parts.flatMap { it.choices }.distinct().sortedBy { it.name }

            // Act
            val index = format.open(bytes = format.write(catalog = catalog))

            // Assert
            assertEquals(expected = VERSION, actual = index.version, message = format.name)
            assertEquals(expected = expected, actual = index.choices, message = format.name)
        }
    }

    @Test
    fun `given every kind of parameter, when each format reads it back, then nothing is lost`() {
        CandidateIndexFormats.all.forEach { format ->
            // Arrange
            val everyKind = catalogOf(component = everyKindComponent())

            // Act
            val index = format.open(bytes = format.write(catalog = everyKind))

            // Assert
            assertEquals(
                expected = everyKindComponent(),
                actual = index.component(name = EVERY_KIND_COMPONENT),
                message = format.name,
            )
        }
    }

    @Test
    fun `given a slot with 128 arguments, when each format reads it back, then every argument is kept`() {
        CandidateIndexFormats.all.forEach { format ->
            // Arrange
            val component = slotComponent(arguments = ABOVE_SIGNED_BYTE_ARGUMENTS)

            // Act
            val index = format.open(bytes = format.write(catalog = catalogOf(component = component)))

            // Assert
            assertEquals(
                expected = component,
                actual = index.component(name = EVERY_KIND_COMPONENT),
                message = format.name,
            )
        }
    }

    @Test
    fun `given a slot with 256 arguments, when each format reads it back, then every argument is kept`() {
        CandidateIndexFormats.all.forEach { format ->
            // Arrange
            val component = slotComponent(arguments = ABOVE_UNSIGNED_BYTE_ARGUMENTS)

            // Act
            val index = format.open(bytes = format.write(catalog = catalogOf(component = component)))

            // Assert
            assertEquals(
                expected = component,
                actual = index.component(name = EVERY_KIND_COMPONENT),
                message = format.name,
            )
        }
    }

    @Test
    fun `given the same catalog, when each format writes it twice, then the bytes are the same`() {
        CandidateIndexFormats.all.forEach { format ->
            // Arrange
            val sameCatalog = catalog.copy()

            // Act
            val first = format.write(catalog = catalog)
            val second = format.write(catalog = sameCatalog)

            // Assert
            assertContentEquals(expected = first, actual = second, message = format.name)
        }
    }

    @Test
    fun `given parts in another order, when each format writes the catalog, then the bytes are the same`() {
        CandidateIndexFormats.all.forEach { format ->
            // Arrange
            val reordered = catalog.copy(parts = catalog.parts.reversed())

            // Act
            val first = format.write(catalog = catalog)
            val second = format.write(catalog = reordered)

            // Assert
            assertContentEquals(expected = first, actual = second, message = format.name)
        }
    }

    @Test
    fun `given one changed component, when each format writes the catalog, then the bytes differ`() {
        CandidateIndexFormats.all.forEach { format ->
            // Arrange
            val changed = catalogOf(component = everyKindComponent().copy(parameters = emptyList()))

            // Act
            val first = format.write(catalog = catalogOf(component = everyKindComponent()))
            val second = format.write(catalog = changed)

            // Assert
            assertFalse(actual = first.contentEquals(second), message = format.name)
        }
    }

    @Test
    fun `given a name the catalog does not hold, when each format looks it up, then it finds nothing`() {
        CandidateIndexFormats.all.forEach { format ->
            // Arrange
            val index = format.open(bytes = format.write(catalog = catalog))

            // Act
            val component = index.component(name = UNKNOWN_COMPONENT)

            // Assert
            assertNull(actual = component, message = format.name)
        }
    }

    @Test
    fun `given bytes of another file, when each format opens them, then it refuses them`() {
        CandidateIndexFormats.all.forEach { format ->
            // Arrange
            val bytes = NOT_AN_INDEX.encodeToByteArray()

            // Act
            val failure = runCatching { format.open(bytes = bytes) }.exceptionOrNull()

            // Assert
            assertIs<IllegalArgumentException>(value = failure, message = format.name)
        }
    }

    private fun slotComponent(arguments: Int) = ComponentDescriptor(
        name = EVERY_KIND_COMPONENT,
        parameters = listOf(
            Parameter.Slot(
                name = "content",
                arguments = List(size = arguments) { ValueKind.Text },
                required = true,
            ),
        ),
    )

    private fun catalogOf(component: ComponentDescriptor) = Catalog(
        version = CatalogVersion(value = VERSION),
        parts = listOf(
            CatalogPart(
                name = PartName(value = "only"),
                choices = listOf(ChoiceList(name = CHOICE_LIST, entries = listOf("First", "Second"))),
                components = listOf(component),
            ),
        ),
    )

    private fun everyKindComponent() = ComponentDescriptor(
        name = EVERY_KIND_COMPONENT,
        parameters = listOf(
            Parameter.Value(name = "text", kind = ValueKind.Text, required = true),
            Parameter.Value(name = "whole", kind = ValueKind.Whole, required = false, nullable = true),
            Parameter.Value(name = "decimal", kind = ValueKind.Decimal, required = true),
            Parameter.Value(name = "flag", kind = ValueKind.Flag, required = false),
            Parameter.Value(name = "choice", kind = ValueKind.Choice(listName = CHOICE_LIST), required = true),
            Parameter.Value(
                name = "choices",
                kind = ValueKind.ListOf(element = ValueKind.Choice(listName = CHOICE_LIST)),
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
        const val COMPONENTS = 300
        const val PARTS = 4
        const val SEED = 123L
        const val VERSION = "2.5 beta"
        const val CHOICE_LIST = "Tone"
        const val EVERY_KIND_COMPONENT = "EveryKind"
        const val UNKNOWN_COMPONENT = "NotInTheCatalog"
        const val NOT_AN_INDEX = "this file is not an index"
        const val ABOVE_SIGNED_BYTE_ARGUMENTS = 128
        const val ABOVE_UNSIGNED_BYTE_ARGUMENTS = 256
    }
}
