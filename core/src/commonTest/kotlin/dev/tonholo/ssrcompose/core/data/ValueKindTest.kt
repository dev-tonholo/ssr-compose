package dev.tonholo.ssrcompose.core.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ValueKindTest {
    @Test
    fun `given a list of bytes, when it is created, then it is rejected`() {
        // Arrange
        val element = ValueKind.Bytes

        // Act
        val create = { ValueKind.ListOf(element = element) }

        // Assert
        assertFailsWith<IllegalArgumentException> { create() }
    }

    @Test
    fun `given a list of lists, when it is created, then it is rejected`() {
        // Arrange
        val element = ValueKind.ListOf(element = ValueKind.Text)

        // Act
        val create = { ValueKind.ListOf(element = element) }

        // Assert
        assertFailsWith<IllegalArgumentException> { create() }
    }

    @Test
    fun `given a list of a choice, when it is created, then it is accepted`() {
        // Arrange
        val element = ValueKind.Choice(listName = "Style")

        // Act
        val actual = ValueKind.ListOf(element = element)

        // Assert
        assertEquals(expected = element, actual = actual.element)
    }
}
