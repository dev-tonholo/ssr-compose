package dev.tonholo.ssrcompose.catalog.validation

import dev.tonholo.ssrcompose.catalog.model.CatalogPart
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.catalog.model.PartName
import dev.tonholo.ssrcompose.core.data.ValueKind
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModelMessageLengthTest {
    @Test
    fun `given a very long component name, when a component is created, then the message is short and marked`() {
        // Arrange
        val prefix = "a".repeat(n = PREFIX_LENGTH)
        val name = prefix + "Z".repeat(n = VERY_LONG - PREFIX_LENGTH)

        // Act
        val create = { ComponentDescriptor(name = name, parameters = emptyList()) }

        // Assert
        assertShortAndMarked(message = assertFailsWith<IllegalArgumentException> { create() }.message, prefix = prefix)
    }

    @Test
    fun `given a very long choice reference in a value, when a catalog is created, then the message is short`() {
        // Arrange
        val prefix = "S".repeat(n = PREFIX_LENGTH)
        val kind = ValueKind.Choice(listName = prefix + "Z".repeat(n = VERY_LONG - PREFIX_LENGTH))
        val parameter = Parameter.Value(name = "style", kind = kind, required = true)

        // Act
        val create = { partWith(parameter = parameter) }

        // Assert
        assertShortAndMarked(message = assertFailsWith<IllegalArgumentException> { create() }.message, prefix = prefix)
    }

    @Test
    fun `given a short name that is not an identifier, when a component is created, then the message keeps it whole`() {
        // Arrange
        val name = "my-card"

        // Act
        val create = { ComponentDescriptor(name = name, parameters = emptyList()) }

        // Assert
        val message = assertFailsWith<IllegalArgumentException> { create() }.message.orEmpty()
        assertTrue(actual = message.contains(other = "\"$name\""), message = message)
    }

    private fun partWith(parameter: Parameter): CatalogPart = CatalogPart(
        name = PartName(value = "app"),
        choices = emptyList(),
        components = listOf(ComponentDescriptor(name = "Card", parameters = listOf(parameter))),
    )

    private fun assertShortAndMarked(message: String?, prefix: String) {
        val text = message.orEmpty()
        assertTrue(actual = text.length < MESSAGE_LIMIT, message = "The message has ${text.length} characters")
        val cutStart = text.indexOf(string = prefix)
        assertTrue(actual = cutStart >= 0, message = text)
        val echoed = text.substring(startIndex = cutStart).removeSuffix(suffix = "\"")
        assertTrue(actual = echoed.length > PREFIX_LENGTH, message = text)
        assertFalse(actual = echoed.contains(other = "Z"), message = text)
    }

    private companion object {
        const val PREFIX_LENGTH = 64
        const val VERY_LONG = 100_000
        const val MESSAGE_LIMIT = 512
    }
}
