package dev.tonholo.ssrcompose.producer.internal.content

import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
import dev.tonholo.ssrcompose.core.message.PlacedComponent
import dev.tonholo.ssrcompose.core.message.ScreenContent
import dev.tonholo.ssrcompose.producer.internal.PlacedNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PlacedNodeContentTest {
    private fun component(name: String, vararg slots: PlacedNode.Slot): PlacedNode.Component =
        PlacedNode.Component(name = name, catalogVersion = CATALOG_VERSION).apply { children += slots }

    private fun slot(name: String, vararg components: PlacedNode.Component): PlacedNode.Slot =
        PlacedNode.Slot(name = name).apply { children += components }

    private fun rootOf(vararg components: PlacedNode.Component): PlacedNode.Root =
        PlacedNode.Root().apply { children += components }

    @Test
    fun `given a slot placed outside a component, when converted to screen content, then it fails naming the slot`() {
        // Arrange
        val root = PlacedNode.Root().apply { children += PlacedNode.Slot(name = HEADER_SLOT) }

        // Act
        val failure = assertFailsWith<IllegalStateException> { root.toScreenContent() }

        // Assert
        assertTrue(actual = HEADER_SLOT in failure.message.orEmpty(), message = failure.message)
    }

    @Test
    fun `given a slot that placed components, when converted to screen content, then the slot holds them in order`() {
        // Arrange
        val root = rootOf(
            component(
                name = CARD,
                slot(name = HEADER_SLOT, component(name = FIRST_LABEL), component(name = SECOND_LABEL)),
            ),
        )
        val expected = ScreenContent.Components(
            components = listOf(
                PlacedComponent(
                    name = CARD,
                    slots = mapOf(
                        HEADER_SLOT to ScreenContent.Components(
                            components = listOf(
                                PlacedComponent(name = FIRST_LABEL),
                                PlacedComponent(name = SECOND_LABEL),
                            ),
                        ),
                    ),
                ),
            ),
        )

        // Act
        val actual = root.toScreenContent()

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a slot that placed nothing, when converted to screen content, then the slot holds no components`() {
        // Arrange
        val root = rootOf(component(name = CARD, slot(name = HEADER_SLOT)))
        val expected = ScreenContent.Components(
            components = listOf(
                PlacedComponent(
                    name = CARD,
                    slots = mapOf(HEADER_SLOT to ScreenContent.Components(components = emptyList())),
                ),
            ),
        )

        // Act
        val actual = root.toScreenContent()

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a slot that was not given, when converted to screen content, then the component has no slots`() {
        // Arrange
        val root = rootOf(component(name = CARD))
        val expected = ScreenContent.Components(components = listOf(PlacedComponent(name = CARD)))

        // Act
        val actual = root.toScreenContent()

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    private companion object {
        const val HEADER_SLOT = "header"
        const val CARD = "Card"
        const val FIRST_LABEL = "First"
        const val SECOND_LABEL = "Second"
        val CATALOG_VERSION = CatalogVersion(value = "1")
    }
}
