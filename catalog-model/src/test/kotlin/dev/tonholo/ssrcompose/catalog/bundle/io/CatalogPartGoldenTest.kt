package dev.tonholo.ssrcompose.catalog.bundle.io

import dev.tonholo.ssrcompose.catalog.bundle.APP_PART_SUBJECT
import dev.tonholo.ssrcompose.catalog.bundle.EVERY_KIND_PART_RESOURCE
import dev.tonholo.ssrcompose.catalog.bundle.SizeCap
import dev.tonholo.ssrcompose.catalog.bundle.TestResources
import dev.tonholo.ssrcompose.catalog.bundle.everyKindPart
import dev.tonholo.ssrcompose.catalog.bundle.everyKindPartInAnotherOrder
import dev.tonholo.ssrcompose.catalog.bundle.partWith
import dev.tonholo.ssrcompose.catalog.model.ChoiceList
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class CatalogPartGoldenTest {
    @Test
    fun `given a part with every kind of parameter, when it is written, then the bytes are the golden file`() {
        // Arrange
        val part = everyKindPart()

        // Act
        val actual = CatalogPartWriter.write(part = part)

        // Assert
        assertContentEquals(expected = TestResources.bytes(name = EVERY_KIND_PART_RESOURCE), actual = actual)
    }

    @Test
    fun `given another order, when the part is written, then the bytes are the golden file`() {
        // Arrange
        val part = everyKindPartInAnotherOrder()

        // Act
        val actual = CatalogPartWriter.write(part = part)

        // Assert
        assertContentEquals(expected = TestResources.bytes(name = EVERY_KIND_PART_RESOURCE), actual = actual)
    }

    @Test
    fun `given mixed case, digit and underscore names, when the part is written, then they are sorted by code unit`() {
        // Arrange
        val names = listOf("b", "B", "A", "a_b", "a1")
        val part = partWith(
            components = names.map { name -> ComponentDescriptor(name = name, parameters = emptyList()) },
            choices = names.map { name -> ChoiceList(name = name, entries = listOf("x")) },
        )

        // Act
        val actual = CatalogPartReader.read(
            bytes = CatalogPartWriter.write(part = part),
            sizeCap = SizeCap.DEFAULT,
            subject = APP_PART_SUBJECT,
        )

        // Assert
        val sorted = listOf("A", "B", "a1", "a_b", "b")
        assertEquals(expected = sorted, actual = actual.components.map { it.name })
        assertEquals(expected = sorted, actual = actual.choices.map { it.name })
    }

    @Test
    fun `given the golden file, when it is read, then it is the part with every kind of parameter`() {
        // Arrange
        val bytes = TestResources.bytes(name = EVERY_KIND_PART_RESOURCE)

        // Act
        val actual = CatalogPartReader.read(bytes = bytes, sizeCap = SizeCap.DEFAULT, subject = APP_PART_SUBJECT)

        // Assert
        assertEquals(expected = everyKindPart(), actual = actual)
    }
}
