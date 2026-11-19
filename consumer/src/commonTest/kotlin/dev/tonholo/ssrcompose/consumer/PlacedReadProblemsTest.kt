package dev.tonholo.ssrcompose.consumer

import dev.tonholo.ssrcompose.consumer.internal.read.PlacedProblemReporter
import dev.tonholo.ssrcompose.core.data.DataValue
import dev.tonholo.ssrcompose.core.message.PlacedComponent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val COMPONENT = "Greeting"
private const val PARAMETER = "parameter"
private const val UNKNOWN_ENTRY = "Huge"

private const val TEXT_STAND_IN = ""
private const val WHOLE_STAND_IN = 0L
private const val DECIMAL_STAND_IN = 0.0
private const val FLAG_STAND_IN = false

private enum class ReadStyle { Body, Title }

private enum class NoEntries

private class FakePlacedProblemReporter : PlacedProblemReporter {
    val components = mutableListOf<String>()

    override fun report(placed: PlacedComponent) {
        components += placed.name
    }
}

private fun sessionScope(
    reporter: PlacedProblemReporter,
    values: Map<String, DataValue> = emptyMap(),
) = PlacedComponentScope(placed = PlacedComponent(name = COMPONENT, values = values), problems = reporter)

class PlacedReadProblemsTest {
    @Test
    fun `given a session and an absent value, when each strict read runs, then it reports and returns its stand-in`() {
        // Arrange
        val reporter = FakePlacedProblemReporter()
        val scope = sessionScope(reporter = reporter)

        // Act
        val text = scope.text(name = PARAMETER)
        val whole = scope.whole(name = PARAMETER)
        val decimal = scope.decimal(name = PARAMETER)
        val flag = scope.flag(name = PARAMETER)
        val list = scope.list(name = PARAMETER)

        // Assert
        assertEquals(expected = TEXT_STAND_IN, actual = text)
        assertEquals(expected = WHOLE_STAND_IN, actual = whole)
        assertEquals(expected = DECIMAL_STAND_IN, actual = decimal)
        assertEquals(expected = FLAG_STAND_IN, actual = flag)
        assertTrue(actual = list.isEmpty())
        assertEquals(expected = List(size = 5) { COMPONENT }, actual = reporter.components)
    }

    @Test
    fun `given a session and another kind, when a strict read runs, then it reports and returns its stand-in`() {
        // Arrange
        val reporter = FakePlacedProblemReporter()
        val scope = sessionScope(
            reporter = reporter,
            values = mapOf(PARAMETER to DataValue.WholeNumber(value = 1)),
        )

        // Act
        val text = scope.text(name = PARAMETER)

        // Assert
        assertEquals(expected = TEXT_STAND_IN, actual = text)
        assertTrue(actual = COMPONENT in reporter.components)
    }

    @Test
    fun `given a session and a value of another kind, when an OrNull read runs, then it reports and returns null`() {
        // Arrange
        val reporter = FakePlacedProblemReporter()
        val scope = sessionScope(
            reporter = reporter,
            values = mapOf(PARAMETER to DataValue.WholeNumber(value = 1)),
        )

        // Act
        val text = scope.textOrNull(name = PARAMETER)

        // Assert
        assertNull(actual = text)
        assertEquals(expected = listOf(COMPONENT), actual = reporter.components)
    }

    @Test
    fun `given a session and an absent value, when an OrNull read runs, then it reports nothing`() {
        // Arrange
        val reporter = FakePlacedProblemReporter()
        val scope = sessionScope(reporter = reporter)

        // Act
        val text = scope.textOrNull(name = PARAMETER)

        // Assert
        assertNull(actual = text)
        assertTrue(actual = reporter.components.isEmpty())
    }

    @Test
    fun `given a session and an absent choice, when the strict read runs, then it returns the first entry`() {
        // Arrange
        val reporter = FakePlacedProblemReporter()
        val scope = sessionScope(reporter = reporter)

        // Act
        val style = scope.choice<ReadStyle>(name = PARAMETER)

        // Assert
        assertEquals(expected = ReadStyle.entries.first(), actual = style)
        assertEquals(expected = listOf(COMPONENT), actual = reporter.components)
    }

    @Test
    fun `given a session and an unknown entry, when the strict read runs, then it returns the first entry`() {
        // Arrange
        val reporter = FakePlacedProblemReporter()
        val scope = sessionScope(
            reporter = reporter,
            values = mapOf(PARAMETER to DataValue.Choice(entry = UNKNOWN_ENTRY)),
        )

        // Act
        val style = scope.choice<ReadStyle>(name = PARAMETER)

        // Assert
        assertEquals(expected = ReadStyle.entries.first(), actual = style)
        assertTrue(actual = COMPONENT in reporter.components)
    }

    @Test
    fun `given a session and an unknown entry, when the OrNull read runs, then it reports and returns null`() {
        // Arrange
        val reporter = FakePlacedProblemReporter()
        val scope = sessionScope(
            reporter = reporter,
            values = mapOf(PARAMETER to DataValue.Choice(entry = UNKNOWN_ENTRY)),
        )

        // Act
        val style = scope.choiceOrNull<ReadStyle>(name = PARAMETER)

        // Assert
        assertNull(actual = style)
        assertEquals(expected = listOf(COMPONENT), actual = reporter.components)
    }

    @Test
    fun `given a session and an enum with no entries, when the strict read runs, then it reports and throws`() {
        // Arrange
        val reporter = FakePlacedProblemReporter()
        val scope = sessionScope(reporter = reporter)

        // Act
        val failure = assertFailsWith<PlacedValueException> { scope.choice<NoEntries>(name = PARAMETER) }

        // Assert
        assertTrue(actual = failure.message.orEmpty().contains(PARAMETER))
        assertEquals(expected = listOf(COMPONENT), actual = reporter.components)
    }
}
