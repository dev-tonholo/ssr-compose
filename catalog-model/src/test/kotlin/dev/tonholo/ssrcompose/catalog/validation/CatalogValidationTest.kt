package dev.tonholo.ssrcompose.catalog.validation

import dev.tonholo.ssrcompose.catalog.model.CatalogPart
import dev.tonholo.ssrcompose.catalog.model.ChoiceList
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.catalog.model.PartName
import dev.tonholo.ssrcompose.core.data.ValueKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CatalogValidationTest {
    @Test
    fun `given a name that is not an identifier, when a component is created, then it is rejected`() {
        // Arrange
        val names = listOf("", "_", "__", "1Card", "my-card", "my card", "Card!")

        // Act
        val failures = names.map { name ->
            runCatching { ComponentDescriptor(name = name, parameters = emptyList()) }.exceptionOrNull()
        }

        // Assert
        val failureTypes = failures.map { failure -> failure?.let { it::class } }
        assertEquals(expected = names.map { IllegalArgumentException::class }, actual = failureTypes)
    }

    @Test
    fun `given a Kotlin keyword, when a component is created, then it is rejected`() {
        // Arrange
        val keywords = listOf("class", "fun", "val", "when", "object", "null", "true")

        // Act
        val results = keywords.map { name ->
            runCatching { ComponentDescriptor(name = name, parameters = emptyList()) }
        }

        // Assert
        assertEquals(expected = keywords.map { false }, actual = results.map { it.isSuccess })
    }

    @Test
    fun `given a name over the maximum length, when a component is created, then it is rejected`() {
        // Arrange
        val name = "A".repeat(n = LONGEST_NAME + 1)

        // Act
        val create = { ComponentDescriptor(name = name, parameters = emptyList()) }

        // Assert
        assertFailsWith<IllegalArgumentException> { create() }
    }

    @Test
    fun `given a name of exactly the maximum length, when a component is created, then it is accepted`() {
        // Arrange
        val name = "A".repeat(n = LONGEST_NAME)

        // Act
        val actual = ComponentDescriptor(name = name, parameters = emptyList())

        // Assert
        assertEquals(expected = name, actual = actual.name)
    }

    @Test
    fun `given a name that is not an identifier, when a choice list is created, then it is rejected`() {
        // Arrange
        val name = "not valid"

        // Act
        val create = { ChoiceList(name = name, entries = listOf("small")) }

        // Assert
        assertFailsWith<IllegalArgumentException> { create() }
    }

    @Test
    fun `given a name that is not an identifier, when any kind of parameter is created, then it is rejected`() {
        // Arrange
        val name = "not valid"
        val creators = listOf(
            { textParameter(name = name) },
            { Parameter.Callback(name = name, required = true) },
            { Parameter.Slot(name = name, required = true) },
        )

        // Act
        val notRejected = creators.indices.filter { index -> runCatching { creators[index]() }.isSuccess }

        // Assert
        assertEquals(expected = emptyList(), actual = notRejected)
    }

    @Test
    fun `given a soft keyword, when a parameter is created, then it is accepted`() {
        // Arrange
        val name = "value"

        // Act
        val actual = textParameter(name = name)

        // Assert
        assertEquals(expected = name, actual = actual.name)
    }

    @Test
    fun `given an entry that is not an identifier, when a choice list is created, then it is rejected`() {
        // Arrange
        val entries = listOf("body medium")

        // Act
        val create = { ChoiceList(name = "Style", entries = entries) }

        // Assert
        assertFailsWith<IllegalArgumentException> { create() }
    }

    @Test
    fun `given a choice list with no entry, when it is created, then it is rejected`() {
        // Arrange
        val entries = emptyList<String>()

        // Act
        val create = { ChoiceList(name = "Style", entries = entries) }

        // Assert
        assertFailsWith<IllegalArgumentException> { create() }
    }

    @Test
    fun `given a repeated entry, when a choice list is created, then it is rejected`() {
        // Arrange
        val entries = listOf("small", "large", "small")

        // Act
        val create = { ChoiceList(name = "Size", entries = entries) }

        // Assert
        assertFailsWith<IllegalArgumentException> { create() }
    }

    @Test
    fun `given a repeated parameter name, when a component is created, then it is rejected`() {
        // Arrange
        val parameters = listOf(textParameter(name = "title"), textParameter(name = "title"))

        // Act
        val create = { ComponentDescriptor(name = "Card", parameters = parameters) }

        // Assert
        assertFailsWith<IllegalArgumentException> { create() }
    }

    @Test
    fun `given a repeated component name, when a catalog is created, then it is rejected`() {
        // Arrange
        val components = listOf(component(name = "Card"), component(name = "Card"))

        // Act
        val create = { partOf(components = components) }

        // Assert
        assertFailsWith<IllegalArgumentException> { create() }
    }

    @Test
    fun `given a repeated choice list name, when a catalog is created, then it is rejected`() {
        // Arrange
        val choices = listOf(
            ChoiceList(name = "Style", entries = listOf("a")),
            ChoiceList(name = "Style", entries = listOf("b")),
        )

        // Act
        val create = { partOf(choices = choices) }

        // Assert
        assertFailsWith<IllegalArgumentException> { create() }
    }

    @Test
    fun `given a choice parameter that names a missing choice list, when a catalog is created, then it is rejected`() {
        // Arrange
        val parameter = Parameter.Value(name = "style", kind = ValueKind.Choice(listName = "Style"), required = true)
        val components = listOf(component(name = "Card", parameters = listOf(parameter)))

        // Act
        val create = { partOf(components = components) }

        // Assert
        assertFailsWith<IllegalArgumentException> { create() }
    }

    @Test
    fun `given a list of a choice that names a missing choice list, when a catalog is created, then it is rejected`() {
        // Arrange
        val kind = ValueKind.ListOf(element = ValueKind.Choice(listName = "Style"))
        val parameter = Parameter.Value(name = "styles", kind = kind, required = true)
        val components = listOf(component(name = "Card", parameters = listOf(parameter)))

        // Act
        val create = { partOf(components = components) }

        // Assert
        assertFailsWith<IllegalArgumentException> { create() }
    }

    @Test
    fun `given a callback argument that names a missing choice list, when a catalog is created, then it is rejected`() {
        // Arrange
        val parameter = Parameter.Callback(
            name = "onPick",
            arguments = listOf(ValueKind.Choice(listName = "Style")),
            required = true,
        )
        val components = listOf(component(name = "Card", parameters = listOf(parameter)))

        // Act
        val create = { partOf(components = components) }

        // Assert
        assertFailsWith<IllegalArgumentException> { create() }
    }

    @Test
    fun `given a slot argument that names a missing choice list, when a catalog is created, then it is rejected`() {
        // Arrange
        val parameter = Parameter.Slot(
            name = "content",
            arguments = listOf(ValueKind.Choice(listName = "Style")),
            required = true,
        )
        val components = listOf(component(name = "Card", parameters = listOf(parameter)))

        // Act
        val create = { partOf(components = components) }

        // Assert
        assertFailsWith<IllegalArgumentException> { create() }
    }

    @Test
    fun `given a choice parameter that names a known choice list, when a catalog is created, then it is accepted`() {
        // Arrange
        val choices = listOf(ChoiceList(name = "Style", entries = listOf("bodyMedium")))
        val parameter = Parameter.Value(name = "style", kind = ValueKind.Choice(listName = "Style"), required = true)
        val components = listOf(component(name = "Card", parameters = listOf(parameter)))

        // Act
        val actual = partOf(choices = choices, components = components)

        // Assert
        assertEquals(expected = 1, actual = actual.components.size)
    }

    @Test
    fun `given a callback with the maximum number of arguments, when it is created, then it is accepted`() {
        // Arrange
        val arguments = List(size = MOST_ARGUMENTS) { ValueKind.Text }

        // Act
        val actual = Parameter.Callback(name = "onDone", arguments = arguments, required = true)

        // Assert
        assertEquals(expected = MOST_ARGUMENTS, actual = actual.arguments.size)
    }

    @Test
    fun `given a callback with more than four arguments, when it is created, then it is rejected`() {
        // Arrange
        val arguments = List(size = MOST_ARGUMENTS + 1) { ValueKind.Text }

        // Act
        val create = { Parameter.Callback(name = "onDone", arguments = arguments, required = true) }

        // Assert
        assertFailsWith<IllegalArgumentException> { create() }
    }

    @Test
    fun `given bytes as a callback argument, when the callback is created, then it is accepted`() {
        // Arrange
        val arguments = listOf(ValueKind.Bytes)

        // Act
        val actual = Parameter.Callback(name = "onScan", arguments = arguments, required = true)

        // Assert
        assertEquals(expected = arguments, actual = actual.arguments)
    }

    @Test
    fun `given bytes as the kind of a value parameter, when it is created, then it is rejected`() {
        // Arrange
        val kind = ValueKind.Bytes

        // Act
        val create = { Parameter.Value(name = "data", kind = kind, required = true) }

        // Assert
        assertFailsWith<IllegalArgumentException> { create() }
    }

    @Test
    fun `given bytes as a slot argument, when the slot is created, then it is rejected`() {
        // Arrange
        val arguments = listOf(ValueKind.Bytes)

        // Act
        val create = { Parameter.Slot(name = "content", arguments = arguments, required = true) }

        // Assert
        assertFailsWith<IllegalArgumentException> { create() }
    }

    private fun textParameter(name: String): Parameter.Value =
        Parameter.Value(name = name, kind = ValueKind.Text, required = true)

    private fun component(name: String, parameters: List<Parameter> = emptyList()): ComponentDescriptor =
        ComponentDescriptor(name = name, parameters = parameters)

    private fun partOf(
        choices: List<ChoiceList> = emptyList(),
        components: List<ComponentDescriptor> = emptyList(),
    ): CatalogPart = CatalogPart(name = PartName(value = "app"), choices = choices, components = components)

    private companion object {
        const val LONGEST_NAME = 64
        const val MOST_ARGUMENTS = 4
    }
}
