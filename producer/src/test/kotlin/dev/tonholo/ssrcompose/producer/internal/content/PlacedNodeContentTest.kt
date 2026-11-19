package dev.tonholo.ssrcompose.producer.internal.content

import dev.tonholo.ssrcompose.producer.internal.PlacedNode
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PlacedNodeContentTest {
    @Test
    fun `given a slot placed outside a component, when converted to screen content, then it fails naming the slot`() {
        // Arrange
        val root = PlacedNode.Root().apply { children += PlacedNode.Slot(name = HEADER_SLOT) }

        // Act
        val failure = assertFailsWith<IllegalStateException> { root.toScreenContent() }

        // Assert
        assertTrue(actual = HEADER_SLOT in failure.message.orEmpty(), message = failure.message)
    }

    private companion object {
        const val HEADER_SLOT = "header"
    }
}
