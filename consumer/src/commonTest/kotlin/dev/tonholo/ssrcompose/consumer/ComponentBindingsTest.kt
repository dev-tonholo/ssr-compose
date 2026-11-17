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
private const val PARAMETER = "parameter"
private const val TEXT_VALUE = "Pay now"
private const val WHOLE_KIND = "whole"
private const val DECIMAL_KIND = "decimal"
private const val TEXT_KIND = "text"
private const val WHOLE_VALUE = 9_000_000_000L
private const val DECIMAL_VALUE = 2.5
private const val CHOICE_KIND = "choice"
private const val LIST_KIND = "list"
private const val UNKNOWN_ENTRY = "Huge"

private enum class Style { Body, Title }

private class ValueRead(
    val expectedKind: String,
    val wrongValue: DataValue,
    val foundKind: String,
    val strict: PlacedComponentScope.() -> Any,
    val orNull: PlacedComponentScope.() -> Any?,
)

private val valueReads = listOf(
    ValueRead(
        expectedKind = TEXT_KIND,
        wrongValue = DataValue.WholeNumber(value = 1),
        foundKind = WHOLE_KIND,
        strict = { text(name = PARAMETER) },
        orNull = { textOrNull(name = PARAMETER) },
    ),
    ValueRead(
        expectedKind = WHOLE_KIND,
        wrongValue = DataValue.DecimalNumber(value = DECIMAL_VALUE),
        foundKind = DECIMAL_KIND,
        strict = { whole(name = PARAMETER) },
        orNull = { wholeOrNull(name = PARAMETER) },
    ),
    ValueRead(
        expectedKind = DECIMAL_KIND,
        wrongValue = DataValue.WholeNumber(value = WHOLE_VALUE),
        foundKind = WHOLE_KIND,
        strict = { decimal(name = PARAMETER) },
        orNull = { decimalOrNull(name = PARAMETER) },
    ),
    ValueRead(
        expectedKind = "flag",
        wrongValue = DataValue.Text(value = TEXT_VALUE),
        foundKind = TEXT_KIND,
        strict = { flag(name = PARAMETER) },
        orNull = { flagOrNull(name = PARAMETER) },
    ),
    ValueRead(
        expectedKind = CHOICE_KIND,
        wrongValue = DataValue.Text(value = TEXT_VALUE),
        foundKind = TEXT_KIND,
        strict = { choice<Style>(name = PARAMETER) },
        orNull = { choiceOrNull<Style>(name = PARAMETER) },
    ),
    ValueRead(
        expectedKind = LIST_KIND,
        wrongValue = DataValue.Text(value = TEXT_VALUE),
        foundKind = TEXT_KIND,
        strict = { list(name = PARAMETER) },
        orNull = { listOrNull(name = PARAMETER) },
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
    PlacedComponent(name = GREETING, values = if (value == null) emptyMap() else mapOf(PARAMETER to value))

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
        val placed = greetingWith(value = DataValue.Text(value = TEXT_VALUE))

        // Act
        val strict = readFrom(placed = placed) { text(name = PARAMETER) }
        val orNull = readFrom(placed = placed) { textOrNull(name = PARAMETER) }

        // Assert
        assertEquals(expected = TEXT_VALUE, actual = strict.getOrThrow())
        assertEquals(expected = TEXT_VALUE, actual = orNull.getOrThrow())
    }

    @Test
    fun `given a whole value beyond the Int range, when it is read as whole, then the same number is returned`() {
        // Arrange
        val placed = greetingWith(value = DataValue.WholeNumber(value = WHOLE_VALUE))

        // Act
        val strict = readFrom(placed = placed) { whole(name = PARAMETER) }
        val orNull = readFrom(placed = placed) { wholeOrNull(name = PARAMETER) }

        // Assert
        assertEquals(expected = WHOLE_VALUE, actual = strict.getOrThrow())
        assertEquals(expected = WHOLE_VALUE, actual = orNull.getOrThrow())
    }

    @Test
    fun `given a decimal value, when it is read as decimal, then the same number is returned`() {
        // Arrange
        val placed = greetingWith(value = DataValue.DecimalNumber(value = DECIMAL_VALUE))

        // Act
        val strict = readFrom(placed = placed) { decimal(name = PARAMETER) }
        val orNull = readFrom(placed = placed) { decimalOrNull(name = PARAMETER) }

        // Assert
        assertEquals(expected = DECIMAL_VALUE, actual = strict.getOrThrow())
        assertEquals(expected = DECIMAL_VALUE, actual = orNull.getOrThrow())
    }

    @Test
    fun `given a flag value, when it is read as flag, then the same flag is returned`() {
        listOf(true, false).forEach { flag ->
            // Arrange
            val placed = greetingWith(value = DataValue.Flag(value = flag))

            // Act
            val strict = readFrom(placed = placed) { flag(name = PARAMETER) }
            val orNull = readFrom(placed = placed) { flagOrNull(name = PARAMETER) }

            // Assert
            assertEquals(expected = flag, actual = strict.getOrThrow())
            assertEquals(expected = flag, actual = orNull.getOrThrow())
        }
    }

    @Test
    fun `given a choice entry of an enum, when it is read as that enum, then the entry is returned`() {
        listOf("Body" to Style.Body, "Title" to Style.Title).forEach { (entry, expected) ->
            // Arrange
            val placed = greetingWith(value = DataValue.Choice(entry = entry))

            // Act
            val strict = readFrom(placed = placed) { choice<Style>(name = PARAMETER) }
            val orNull = readFrom(placed = placed) { choiceOrNull<Style>(name = PARAMETER) }

            // Assert
            assertEquals(expected = expected, actual = strict.getOrThrow())
            assertEquals(expected = expected, actual = orNull.getOrThrow())
        }
    }

    @Test
    fun `given a choice entry the enum does not have, when it is read, then both reads name the entry and the enum`() {
        // Arrange
        val placed = greetingWith(value = DataValue.Choice(entry = UNKNOWN_ENTRY))

        // Act
        val strict = readFrom(placed = placed) { choice<Style>(name = PARAMETER) }
        val orNull = readFrom(placed = placed) { choiceOrNull<Style>(name = PARAMETER) }

        // Assert
        listOf(strict, orNull).forEach { result ->
            val message = assertIs<PlacedValueException>(result.exceptionOrNull()).message.orEmpty()
            assertTrue(
                actual = listOf(GREETING, PARAMETER, UNKNOWN_ENTRY, "Style").all { it in message },
                message = message,
            )
        }
    }

    @Test
    fun `given a list value, when it is read as a list, then its items are returned in order`() {
        // Arrange
        val items = listOf(DataValue.Text(value = TEXT_VALUE), DataValue.WholeNumber(value = WHOLE_VALUE))
        val placed = greetingWith(value = DataValue.ListValue(items = items))

        // Act
        val strict = readFrom(placed = placed) { list(name = PARAMETER) }
        val orNull = readFrom(placed = placed) { listOrNull(name = PARAMETER) }

        // Assert
        assertEquals(expected = items, actual = strict.getOrThrow())
        assertEquals(expected = items, actual = orNull.getOrThrow())
    }

    @Test
    fun `given an empty list value, when it is read as a list, then an empty list is returned`() {
        // Arrange
        val placed = greetingWith(value = DataValue.ListValue(items = emptyList()))

        // Act
        val strict = readFrom(placed = placed) { list(name = PARAMETER) }
        val orNull = readFrom(placed = placed) { listOrNull(name = PARAMETER) }

        // Assert
        assertEquals(expected = emptyList(), actual = strict.getOrThrow())
        assertEquals(expected = emptyList(), actual = orNull.getOrThrow())
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
            assertTrue(actual = listOf(GREETING, PARAMETER, read.expectedKind).all { it in failure.message.orEmpty() })
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
                    actual = listOf(GREETING, PARAMETER, read.expectedKind, read.foundKind).all { it in message },
                    message = message,
                )
            }
        }
    }
}
