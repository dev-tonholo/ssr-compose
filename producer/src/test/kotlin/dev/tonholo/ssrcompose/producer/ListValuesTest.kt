@file:OptIn(InternalSsrComposeApi::class)

package dev.tonholo.ssrcompose.producer

import dev.tonholo.ssrcompose.core.data.DataValue
import kotlin.test.Test
import kotlin.test.assertEquals

class ListValuesTest {
    @Test
    fun `given texts, when textListValue is called, then each becomes a Text in order`() {
        // Arrange
        val expected = DataValue.ListValue(
            items = listOf(DataValue.Text(value = FIRST_TEXT), DataValue.Text(value = SECOND_TEXT)),
        )

        // Act
        val actual = textListValue(items = listOf(FIRST_TEXT, SECOND_TEXT))

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given whole numbers, when wholeListValue is called, then each becomes a WholeNumber in order`() {
        // Arrange
        val expected = DataValue.ListValue(
            items = listOf(DataValue.WholeNumber(value = FIRST_WHOLE), DataValue.WholeNumber(value = SECOND_WHOLE)),
        )

        // Act
        val actual = wholeListValue(items = listOf(FIRST_WHOLE, SECOND_WHOLE))

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given decimal numbers, when decimalListValue is called, then each becomes a DecimalNumber in order`() {
        // Arrange
        val expected = DataValue.ListValue(
            items = listOf(
                DataValue.DecimalNumber(value = FIRST_DECIMAL),
                DataValue.DecimalNumber(value = SECOND_DECIMAL),
            ),
        )

        // Act
        val actual = decimalListValue(items = listOf(FIRST_DECIMAL, SECOND_DECIMAL))

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given flags, when flagListValue is called, then each becomes a Flag in order`() {
        // Arrange
        val expected = DataValue.ListValue(
            items = listOf(DataValue.Flag(value = true), DataValue.Flag(value = false)),
        )

        // Act
        val actual = flagListValue(items = listOf(true, false))

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given enum entries, when choiceListValue is called, then each becomes a Choice of its name in order`() {
        // Arrange
        val expected = DataValue.ListValue(
            items = listOf(DataValue.Choice(entry = TITLE_ENTRY), DataValue.Choice(entry = BODY_ENTRY)),
        )

        // Act
        val actual = choiceListValue(items = listOf(Style.Title, Style.Body))

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    private enum class Style { Body, Title }

    private companion object {
        const val FIRST_TEXT = "first"
        const val SECOND_TEXT = "second"
        const val FIRST_WHOLE = 7L
        const val SECOND_WHOLE = -3L
        const val FIRST_DECIMAL = 1.5
        const val SECOND_DECIMAL = -0.25
        const val TITLE_ENTRY = "Title"
        const val BODY_ENTRY = "Body"
    }
}
