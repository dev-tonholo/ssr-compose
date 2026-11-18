package dev.tonholo.ssrcompose.catalog.index

import dev.tonholo.ssrcompose.catalog.bundle.CatalogFileException
import dev.tonholo.ssrcompose.catalog.model.Catalog
import dev.tonholo.ssrcompose.catalog.model.CatalogPart
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.catalog.model.PartName
import dev.tonholo.ssrcompose.catalog.testing.HandEncodedChoice
import dev.tonholo.ssrcompose.catalog.testing.HandEncodedComponent
import dev.tonholo.ssrcompose.catalog.testing.HandEncodedIndex
import dev.tonholo.ssrcompose.catalog.testing.HandEncodedIndex.EMPTY_BODY
import dev.tonholo.ssrcompose.catalog.testing.HandEncodedIndex.FLAG_NULLABLE
import dev.tonholo.ssrcompose.catalog.testing.HandEncodedIndex.FLAG_REQUIRED
import dev.tonholo.ssrcompose.catalog.testing.HandEncodedIndex.TEXT_KIND
import dev.tonholo.ssrcompose.catalog.testing.HandEncodedIndex.bodyOf
import dev.tonholo.ssrcompose.catalog.testing.HandEncodedIndex.callback
import dev.tonholo.ssrcompose.catalog.testing.HandEncodedIndex.choiceKind
import dev.tonholo.ssrcompose.catalog.testing.HandEncodedIndex.listKind
import dev.tonholo.ssrcompose.catalog.testing.HandEncodedIndex.slot
import dev.tonholo.ssrcompose.catalog.testing.HandEncodedIndex.value
import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
import dev.tonholo.ssrcompose.core.data.ValueKind
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CatalogIndexDamageTest {
    private val bodySize = EMPTY_BODY.size

    @Test
    fun `given two components at one body offset, when the index is opened, then it is refused`() {
        // Arrange
        val bytes = threeComponents(offsets = listOf(0, 0, bodySize), bodies = EMPTY_BODY + EMPTY_BODY)

        // Act
        val open = { CatalogIndexReader.read(bytes = bytes) }

        // Assert
        assertFailsWith<CatalogFileException> { open() }
    }

    @Test
    fun `given a first body offset above zero, when the index is opened, then it is refused`() {
        // Arrange
        val offsets = listOf(bodySize, 2 * bodySize, 3 * bodySize)
        val bytes = threeComponents(offsets = offsets, bodies = EMPTY_BODY + EMPTY_BODY + EMPTY_BODY + EMPTY_BODY)

        // Act
        val open = { CatalogIndexReader.read(bytes = bytes) }

        // Assert
        assertFailsWith<CatalogFileException> { open() }
    }

    @Test
    fun `given body offsets that go down after a valid first offset, when the index is opened, then it is refused`() {
        // Arrange
        val bytes = threeComponents(
            offsets = listOf(0, 2 * bodySize, bodySize),
            bodies = EMPTY_BODY + EMPTY_BODY + EMPTY_BODY,
        )

        // Act
        val open = { CatalogIndexReader.read(bytes = bytes) }

        // Assert
        assertFailsWith<CatalogFileException> { open() }
    }

    @Test
    fun `given a last body offset at the end of the bodies, when the index is opened, then it is refused`() {
        // Arrange
        val bytes = threeComponents(offsets = listOf(0, bodySize, 2 * bodySize), bodies = EMPTY_BODY + EMPTY_BODY)

        // Act
        val open = { CatalogIndexReader.read(bytes = bytes) }

        // Assert
        assertFailsWith<CatalogFileException> { open() }
    }

    @Test
    fun `given a gap after a body, when that component is read, then it is refused`() {
        // Arrange
        val bytes = threeComponents(
            offsets = listOf(0, 2 * bodySize, 3 * bodySize),
            bodies = EMPTY_BODY + EMPTY_BODY + EMPTY_BODY + EMPTY_BODY,
        )
        val index = CatalogIndexReader.read(bytes = bytes)

        // Act
        val read = { index.component(name = ALPHA) }

        // Assert
        assertFailsWith<CatalogFileException> { read() }
    }

    @Test
    fun `given a body that runs into the next body, when that component is read, then it is refused`() {
        // Arrange
        val longBody = bodyOf(value(nameId = ALPHA_ID))
        val bytes = threeComponents(
            offsets = listOf(0, bodySize, 2 * bodySize),
            bodies = longBody + EMPTY_BODY,
        )
        val index = CatalogIndexReader.read(bytes = bytes)

        // Act
        val read = { index.component(name = ALPHA) }

        // Assert
        assertFailsWith<CatalogFileException> { read() }
    }

    @Test
    fun `given body bytes and no component, when the index is opened, then it is refused`() {
        // Arrange
        val bytes = HandEncodedIndex.encode(strings = listOf(VERSION), components = emptyList(), bodies = EMPTY_BODY)

        // Act
        val open = { CatalogIndexReader.read(bytes = bytes) }

        // Assert
        assertFailsWith<CatalogFileException> { open() }
    }

    @Test
    fun `given thousands of components sharing one large body, when the index is opened, then it is refused`() {
        // Arrange
        val names = List(size = SHARING_COMPONENTS) { index ->
            "C${index.toString().padStart(length = SHARING_NAME_DIGITS, padChar = '0')}".encodeToByteArray()
        }
        val parameters = Array(size = SHARED_BODY_PARAMETERS) { value(nameId = 1) }
        val bytes = HandEncodedIndex.encode(
            strings = listOf(VERSION) + names,
            components = names.indices.map { index -> HandEncodedComponent(nameId = index + 1, bodyOffset = 0) },
            bodies = bodyOf(*parameters),
        )

        // Act
        val open = { CatalogIndexReader.read(bytes = bytes) }

        // Assert
        assertFailsWith<CatalogFileException> { open() }
    }

    @Test
    fun `given one component name twice with valid offsets, when the index is opened, then it is refused`() {
        // Arrange
        val bytes = componentsNamed(nameIds = listOf(ALPHA_ID, BETA_ID, BETA_ID))

        // Act
        val open = { CatalogIndexReader.read(bytes = bytes) }

        // Assert
        assertFailsWith<CatalogFileException> { open() }
    }

    @Test
    fun `given distinct component names at those offsets, when the index is opened, then it holds them all`() {
        // Arrange
        val bytes = componentsNamed(nameIds = listOf(ALPHA_ID, BETA_ID, GAMMA_ID))

        // Act
        val index = CatalogIndexReader.read(bytes = bytes)

        // Assert
        assertEquals(expected = listOf(ALPHA, BETA, GAMMA), actual = index.componentNames)
    }

    @Test
    fun `given component names out of order, when the index is opened, then it is refused`() {
        // Arrange
        val bytes = componentsNamed(nameIds = listOf(BETA_ID, ALPHA_ID, GAMMA_ID))

        // Act
        val open = { CatalogIndexReader.read(bytes = bytes) }

        // Assert
        assertFailsWith<CatalogFileException> { open() }
    }

    @Test
    fun `given one choice list name twice, when the index is opened, then it is refused`() {
        // Arrange
        val bytes = HandEncodedIndex.encode(
            strings = STRINGS,
            choices = listOf(
                HandEncodedChoice(nameId = TONE_ID, entryIds = listOf(ALPHA_ID)),
                HandEncodedChoice(nameId = TONE_ID, entryIds = listOf(BETA_ID)),
            ),
            components = listOf(HandEncodedComponent(nameId = ALPHA_ID, bodyOffset = 0)),
            bodies = EMPTY_BODY,
        )

        // Act
        val open = { CatalogIndexReader.read(bytes = bytes) }

        // Assert
        assertFailsWith<CatalogFileException> { open() }
    }

    @Test
    fun `given a catalog version that is not valid UTF-8, when the index is opened, then it is refused`() {
        // Arrange
        val bytes = HandEncodedIndex.encode(
            strings = listOf(byteArrayOf(INVALID_UTF8_BYTE), ALPHA.encodeToByteArray()),
            components = listOf(HandEncodedComponent(nameId = ALPHA_ID, bodyOffset = 0)),
            bodies = EMPTY_BODY,
        )

        // Act
        val open = { CatalogIndexReader.read(bytes = bytes) }

        // Assert
        assertFailsWith<CatalogFileException> { open() }
    }

    @Test
    fun `given a parameter with an unknown flag, when the component is read, then it is refused`() {
        // Arrange
        val index = indexOfOneComponent(body = bodyOf(value(nameId = TITLE_ID, flags = UNKNOWN_FLAG)))

        // Act
        val read = { index.component(name = ALPHA) }

        // Assert
        assertFailsWith<CatalogFileException> { read() }
    }

    @TestFactory
    fun `given the nullable flag on a callback or a slot, when the component is read, then it is refused`() =
        mapOf(
            "callback" to callback(nameId = TITLE_ID, kinds = emptyList(), flags = REQUIRED_AND_NULLABLE),
            "slot" to slot(nameId = TITLE_ID, flags = REQUIRED_AND_NULLABLE),
        ).map { (shape, parameter) ->
            dynamicTest(shape) {
                // Arrange
                val index = indexOfOneComponent(body = bodyOf(parameter))

                // Act
                val read = { index.component(name = ALPHA) }

                // Assert
                assertFailsWith<CatalogFileException> { read() }
            }
        }

    @Test
    fun `given a callback over the argument limit, when the component is read, then its arguments are not decoded`() {
        // Arrange
        val kinds = List(size = OVER_CALLBACK_LIMIT) { TEXT_KIND }
        val index = indexOfOneComponent(body = bodyOf(callback(nameId = TITLE_ID, kinds = kinds)))

        // Act
        val failure = assertFailsWith<CatalogFileException> { index.component(name = ALPHA) }

        // Assert
        assertTrue(actual = failure.message.orEmpty().contains(DAMAGED), message = failure.message)
    }

    @TestFactory
    fun `given a reference to a choice list the index does not hold, when the component is read, then it is refused`() =
        mapOf(
            "value" to value(nameId = TITLE_ID, kind = choiceKind(listNameId = MISSING_LIST_ID)),
            "list element" to value(nameId = TITLE_ID, kind = listKind(choiceKind(listNameId = MISSING_LIST_ID))),
            "callback" to callback(nameId = TITLE_ID, kinds = listOf(choiceKind(listNameId = MISSING_LIST_ID))),
            "slot" to slot(nameId = TITLE_ID, kinds = listOf(choiceKind(listNameId = MISSING_LIST_ID))),
        ).map { (shape, parameter) ->
            dynamicTest(shape) {
                // Arrange
                val index = indexOfOneComponent(body = bodyOf(parameter))

                // Act
                val read = { index.component(name = ALPHA) }

                // Assert
                assertFailsWith<CatalogFileException> { read() }
            }
        }

    @Test
    fun `given a reference to a choice list the index holds, when the component is read, then it is kept`() {
        // Arrange
        val parameter = value(nameId = TITLE_ID, kind = choiceKind(listNameId = TONE_ID))
        val index = indexOfOneComponent(body = bodyOf(parameter))
        val expected = Parameter.Value(name = "title", kind = ValueKind.Choice(listName = "Tone"), required = true)

        // Act
        val component = index.component(name = ALPHA)

        // Assert
        assertEquals(expected = listOf<Parameter>(expected), actual = component?.parameters)
    }

    @Test
    fun `given an index opened from an array, when the array changes, then the component stays the same`() {
        // Arrange
        val body = bodyOf(value(nameId = TITLE_ID))
        val bytes = HandEncodedIndex.encode(
            strings = STRINGS,
            components = listOf(HandEncodedComponent(nameId = ALPHA_ID, bodyOffset = 0)),
            bodies = body,
        )
        val index = CatalogIndexReader.read(bytes = bytes)

        // Act
        bytes[bytes.size - FLAGS_FROM_BODY_END] = 0

        // Assert
        assertEquals(expected = true, actual = index.component(name = ALPHA)?.parameters?.single()?.required)
    }

    @Test
    fun `given an index larger than 1 MiB, when it is read with the default cap, then it opens`() {
        // Arrange
        val names = List(size = LARGE_CATALOG_COMPONENTS) { index ->
            "C" + index.toString().padStart(length = LONGEST_NAME - 1, padChar = '0')
        }
        val bytes = CatalogIndexWriter.write(catalog = catalogNaming(names = names))
        assertTrue(actual = bytes.size > ONE_MEBIBYTE, message = "${bytes.size} bytes")

        // Act
        val index = CatalogIndexReader.read(stream = ByteArrayInputStream(bytes))

        // Assert
        assertEquals(expected = names, actual = index.componentNames)
    }

    private fun threeComponents(offsets: List<Int>, bodies: ByteArray): ByteArray =
        HandEncodedIndex.encode(
            strings = STRINGS,
            components = listOf(ALPHA_ID, BETA_ID, GAMMA_ID).zip(offsets) { nameId, offset ->
                HandEncodedComponent(nameId = nameId, bodyOffset = offset)
            },
            bodies = bodies,
        )

    private fun componentsNamed(nameIds: List<Int>): ByteArray =
        HandEncodedIndex.encode(
            strings = STRINGS,
            components = nameIds.mapIndexed { position, nameId ->
                HandEncodedComponent(nameId = nameId, bodyOffset = position * bodySize)
            },
            bodies = EMPTY_BODY + EMPTY_BODY + EMPTY_BODY,
        )

    private fun indexOfOneComponent(body: ByteArray): CatalogIndex =
        CatalogIndexReader.read(
            bytes = HandEncodedIndex.encode(
                strings = STRINGS,
                choices = listOf(HandEncodedChoice(nameId = TONE_ID, entryIds = listOf(ALPHA_ID))),
                components = listOf(HandEncodedComponent(nameId = ALPHA_ID, bodyOffset = 0)),
                bodies = body,
            ),
        )

    private fun catalogNaming(names: List<String>) = Catalog(
        version = CatalogVersion(value = "1"),
        parts = listOf(
            CatalogPart(
                name = PartName(value = "only"),
                choices = emptyList(),
                components = names.map { name -> ComponentDescriptor(name = name, parameters = emptyList()) },
            ),
        ),
    )

    private companion object {
        const val ALPHA = "Alpha"
        const val BETA = "Beta"
        const val GAMMA = "Gamma"
        const val ALPHA_ID = 1
        const val BETA_ID = 2
        const val GAMMA_ID = 3
        const val TITLE_ID = 4
        const val TONE_ID = 5
        const val MISSING_LIST_ID = 6
        const val DAMAGED = "damaged"
        const val UNKNOWN_FLAG = 4
        const val REQUIRED_AND_NULLABLE = FLAG_REQUIRED or FLAG_NULLABLE
        const val OVER_CALLBACK_LIMIT = 5
        const val FLAGS_FROM_BODY_END = 2
        const val INVALID_UTF8_BYTE: Byte = -1
        const val SHARING_COMPONENTS = 4_000
        const val SHARING_NAME_DIGITS = 4
        const val SHARED_BODY_PARAMETERS = 20_000
        const val LARGE_CATALOG_COMPONENTS = 20_000
        const val LONGEST_NAME = 64
        const val ONE_MEBIBYTE = 1024 * 1024
        val VERSION = "1".encodeToByteArray()
        val STRINGS = listOf("1", ALPHA, BETA, GAMMA, "title", "Tone", "Missing").map { it.encodeToByteArray() }
    }
}
