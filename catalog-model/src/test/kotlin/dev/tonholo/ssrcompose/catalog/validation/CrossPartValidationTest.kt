package dev.tonholo.ssrcompose.catalog.validation

import dev.tonholo.ssrcompose.catalog.model.Catalog
import dev.tonholo.ssrcompose.catalog.model.CatalogPart
import dev.tonholo.ssrcompose.catalog.model.ChoiceList
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.PartName
import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CrossPartValidationTest {
    @Test
    fun `given a component name used in two parts, when a catalog is created, then it is rejected naming both`() {
        // Arrange
        val parts = listOf(
            part(name = "app", components = listOf("Card")),
            part(name = "home", components = listOf("Card")),
        )

        // Act
        val create = { Catalog(version = CatalogVersion(value = "1"), parts = parts) }

        // Assert
        val message = assertFailsWith<IllegalArgumentException> { create() }.message.orEmpty()
        assertTrue(actual = message.contains(other = "app") && message.contains(other = "home"), message = message)
    }

    @Test
    fun `given a choice list that differs between two parts, when a catalog is created, then each is rejected`() {
        // Arrange
        val first = ChoiceList(name = "Style", entries = listOf("small", "large"))
        val differing = listOf(
            ChoiceList(name = "Style", entries = listOf("small", "huge")),
            ChoiceList(name = "Style", entries = listOf("large", "small")),
            ChoiceList(name = "Style", entries = listOf("small")),
        )

        // Act
        val messages = differing.map { other ->
            val parts = listOf(
                part(name = "app", choices = listOf(first)),
                part(name = "home", choices = listOf(other)),
            )
            runCatching { Catalog(version = CatalogVersion(value = "1"), parts = parts) }.exceptionOrNull()?.message
        }

        // Assert
        val named = messages.filter { it.orEmpty().contains(other = "app") && it.orEmpty().contains(other = "home") }
        assertEquals(expected = differing.size, actual = named.size, message = messages.toString())
    }

    @Test
    fun `given the same choice list in two parts, when a catalog is created, then it is accepted`() {
        // Arrange
        val style = ChoiceList(name = "Style", entries = listOf("small", "large"))
        val parts = listOf(part(name = "app", choices = listOf(style)), part(name = "home", choices = listOf(style)))

        // Act
        val actual = Catalog(version = CatalogVersion(value = "1"), parts = parts)

        // Assert
        assertEquals(expected = parts, actual = actual.parts)
    }

    @Test
    fun `given a part name used twice, when a catalog is created, then it is rejected`() {
        // Arrange
        val parts = listOf(part(name = "app"), part(name = "app"))

        // Act
        val create = { Catalog(version = CatalogVersion(value = "1"), parts = parts) }

        // Assert
        assertFailsWith<IllegalArgumentException> { create() }
    }

    private fun part(
        name: String,
        components: List<String> = emptyList(),
        choices: List<ChoiceList> = emptyList(),
    ): CatalogPart = CatalogPart(
        name = PartName(value = name),
        choices = choices,
        components = components.map { ComponentDescriptor(name = it, parameters = emptyList()) },
    )
}
