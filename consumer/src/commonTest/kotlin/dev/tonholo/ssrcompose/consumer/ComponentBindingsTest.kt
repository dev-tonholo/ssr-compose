package dev.tonholo.ssrcompose.consumer

import dev.tonholo.ssrcompose.consumer.testing.composeOnce
import dev.tonholo.ssrcompose.core.data.DataValue
import dev.tonholo.ssrcompose.core.message.PlacedComponent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val GREETING = "Greeting"
private const val PAYMENT_SECTION = "PaymentSection"
private const val TITLE = "title"
private const val TITLE_TEXT = "Pay now"
private const val WHOLE_KIND = "whole"

private class ValueRead(
    val expectedKind: String,
    val wrongValue: DataValue,
    val foundKind: String,
    val strict: PlacedComponentScope.() -> Any,
    val orNull: PlacedComponentScope.() -> Any?,
)

private val valueReads = listOf(
    ValueRead(
        expectedKind = "text",
        wrongValue = DataValue.WholeNumber(value = 1),
        foundKind = WHOLE_KIND,
        strict = { text(name = TITLE) },
        orNull = { textOrNull(name = TITLE) },
    ),
)

private fun <T> readFrom(placed: PlacedComponent, read: PlacedComponentScope.() -> T): Result<T> {
    var result: Result<T>? = null
    val bindings = ComponentBindings {
        bind(name = placed.name) { scope -> result = runCatching { scope.read() } }
    }
    val binding = assertNotNull(bindings.find(name = placed.name))
    composeOnce { binding(PlacedComponentScope(placed = placed)) }
    return assertNotNull(result)
}

private fun greetingWith(value: DataValue?): PlacedComponent =
    PlacedComponent(name = GREETING, values = if (value == null) emptyMap() else mapOf(TITLE to value))

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

    @Test
    fun `given a text value, when it is read as text, then the text is returned`() {
        // Arrange
        val placed = greetingWith(value = DataValue.Text(value = TITLE_TEXT))

        // Act
        val strict = readFrom(placed = placed) { text(name = TITLE) }
        val orNull = readFrom(placed = placed) { textOrNull(name = TITLE) }

        // Assert
        assertEquals(expected = TITLE_TEXT, actual = strict.getOrThrow())
        assertEquals(expected = TITLE_TEXT, actual = orNull.getOrThrow())
    }

    @Test
    fun `given an absent parameter, when it is read, then strict reads name it and OrNull reads give null`() {
        // Arrange
        val placed = greetingWith(value = null)

        valueReads.forEach { read ->
            // Act
            val strict = readFrom(placed = placed, read = read.strict)
            val orNull = readFrom(placed = placed, read = read.orNull)

            // Assert
            val failure = assertIs<PlacedValueException>(value = strict.exceptionOrNull(), message = read.expectedKind)
            assertTrue(actual = listOf(GREETING, TITLE, read.expectedKind).all { it in failure.message.orEmpty() })
            assertNull(actual = orNull.getOrThrow(), message = read.expectedKind)
        }
    }

    @Test
    fun `given a parameter set to none, when it is read, then strict reads fail and OrNull reads give null`() {
        // Arrange
        val placed = greetingWith(value = DataValue.None)

        valueReads.forEach { read ->
            // Act
            val strict = readFrom(placed = placed, read = read.strict)
            val orNull = readFrom(placed = placed, read = read.orNull)

            // Assert
            assertIs<PlacedValueException>(value = strict.exceptionOrNull(), message = read.expectedKind)
            assertNull(actual = orNull.getOrThrow(), message = read.expectedKind)
        }
    }

    @Test
    fun `given a value of another kind, when it is read, then both reads name the expected and found kinds`() {
        valueReads.forEach { read ->
            // Arrange
            val placed = greetingWith(value = read.wrongValue)

            // Act
            val strict = readFrom(placed = placed, read = read.strict)
            val orNull = readFrom(placed = placed, read = read.orNull)

            // Assert
            listOf(strict, orNull).forEach { result ->
                val failure = assertIs<PlacedValueException>(result.exceptionOrNull(), message = read.expectedKind)
                val message = failure.message.orEmpty()
                assertTrue(
                    actual = listOf(GREETING, TITLE, read.expectedKind, read.foundKind).all { it in message },
                    message = message,
                )
            }
        }
    }
}
