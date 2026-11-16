package dev.tonholo.ssrcompose.catalog.validation

import dev.tonholo.ssrcompose.catalog.model.Catalog
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
import dev.tonholo.ssrcompose.core.data.ValueKind
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModelMessageLengthTest {
    @Test
    fun `given a very long component name, when a component is created, then the message is short and marked`() {
        // Arrange
        val name = "a".repeat(n = VERY_LONG)

        // Act
        val create = { ComponentDescriptor(name = name, parameters = emptyList()) }

        // Assert
        assertShortAndMarked(message = assertFailsWith<IllegalArgumentException> { create() }.message)
    }

    @Test
    fun `given a very long choice reference in a value, when a catalog is created, then the message is short`() {
        // Arrange
        val kind = ValueKind.Choice(listName = "S".repeat(n = VERY_LONG))
        val parameter = Parameter.Value(name = "style", kind = kind, required = true)

        // Act
        val create = { catalogWith(parameter = parameter) }

        // Assert
        assertShortAndMarked(message = assertFailsWith<IllegalArgumentException> { create() }.message)
    }

    @Test
    fun `given a short name that is not an identifier, when a component is created, then the message keeps it whole`() {
        // Arrange
        val name = "my-card"

        // Act
        val create = { ComponentDescriptor(name = name, parameters = emptyList()) }

        // Assert
        val message = assertFailsWith<IllegalArgumentException> { create() }.message.orEmpty()
        assertTrue(actual = message.contains(other = name), message = message)
        assertFalse(actual = message.contains(other = EchoedText.CUT_MARKER), message = message)
    }

    private fun catalogWith(parameter: Parameter): Catalog = Catalog(
        version = CatalogVersion(value = "1"),
        choices = emptyList(),
        components = listOf(ComponentDescriptor(name = "Card", parameters = listOf(parameter))),
    )

    private fun assertShortAndMarked(message: String?) {
        val text = message.orEmpty()
        assertTrue(actual = text.length < MESSAGE_LIMIT, message = "The message has ${text.length} characters")
        assertTrue(actual = text.contains(other = EchoedText.CUT_MARKER), message = text)
    }

    private companion object {
        const val VERY_LONG = 100_000
        const val MESSAGE_LIMIT = 512
    }
}
