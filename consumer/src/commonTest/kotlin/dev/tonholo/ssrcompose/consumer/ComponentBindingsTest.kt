package dev.tonholo.ssrcompose.consumer

import dev.tonholo.ssrcompose.consumer.testing.composeOnce
import dev.tonholo.ssrcompose.core.message.PlacedComponent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val GREETING = "Greeting"
private const val PAYMENT_SECTION = "PaymentSection"

class ComponentBindingsTest {
    @Test
    fun `given a binding for a component, when it is found by name and composed, then only that binding runs`() {
        // Arrange
        val ran = mutableListOf<String>()
        val bindings = ComponentBindings {
            bind(name = GREETING) { ran += GREETING }
            bind(name = PAYMENT_SECTION) { ran += PAYMENT_SECTION }
        }
        val placed = PlacedComponent(name = GREETING)

        // Act
        val binding = assertNotNull(bindings.find(name = GREETING))
        composeOnce { binding(PlacedComponentScope(placed = placed)) }

        // Assert
        assertEquals(expected = listOf(GREETING), actual = ran)
    }

    @Test
    fun `given no binding for a name, when it is looked up, then nothing is found`() {
        // Arrange
        val bindings = ComponentBindings { bind(name = GREETING) { } }

        // Act
        val actual = bindings.find(name = PAYMENT_SECTION)

        // Assert
        assertNull(actual = actual)
    }

    @Test
    fun `given a name that is already bound, when it is bound again, then it is rejected with the name`() {
        // Arrange
        val declare = {
            ComponentBindings {
                bind(name = GREETING) { }
                bind(name = GREETING) { }
            }
        }

        // Act
        val failure = assertFailsWith<IllegalArgumentException> { declare() }

        // Assert
        assertTrue(actual = GREETING in failure.message.orEmpty())
    }
}
