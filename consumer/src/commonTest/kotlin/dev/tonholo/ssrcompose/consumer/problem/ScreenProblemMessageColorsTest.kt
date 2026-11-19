package dev.tonholo.ssrcompose.consumer.problem

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

private val CONTAINER = Color.Red
private val CONTENT = Color.Green
private val OTHER_CONTAINER = Color.Blue
private val OTHER_CONTENT = Color.Yellow

class ScreenProblemMessageColorsTest {
    @Test
    fun `given colors with the same pair, when compared, then they are equal and share a hash code`() {
        // Arrange
        val first = ScreenProblemMessageColors(containerColor = CONTAINER, contentColor = CONTENT)
        val second = ScreenProblemMessageColors(containerColor = CONTAINER, contentColor = CONTENT)

        // Act
        val equal = first == second
        val sameHash = first.hashCode() == second.hashCode()

        // Assert
        assertTrue(actual = equal)
        assertTrue(actual = sameHash)
    }

    @Test
    fun `given colors that differ in one color, when compared, then they are not equal`() {
        // Arrange
        val colors = ScreenProblemMessageColors(containerColor = CONTAINER, contentColor = CONTENT)
        val otherContainer = ScreenProblemMessageColors(containerColor = OTHER_CONTAINER, contentColor = CONTENT)
        val otherContent = ScreenProblemMessageColors(containerColor = CONTAINER, contentColor = OTHER_CONTENT)

        // Act
        val sameWithOtherContainer = colors == otherContainer
        val sameWithOtherContent = colors == otherContent

        // Assert
        assertFalse(actual = sameWithOtherContainer)
        assertFalse(actual = sameWithOtherContent)
    }

    @Test
    fun `given colors, when copied with unspecified colors, then the old colors are kept`() {
        // Arrange
        val colors = ScreenProblemMessageColors(containerColor = CONTAINER, contentColor = CONTENT)

        // Act
        val copy = colors.copy(containerColor = Color.Unspecified, contentColor = Color.Unspecified)

        // Assert
        assertEquals(expected = colors, actual = copy)
    }

    @Test
    fun `given colors, when copied with new colors, then only the given colors change`() {
        // Arrange
        val colors = ScreenProblemMessageColors(containerColor = CONTAINER, contentColor = CONTENT)

        // Act
        val newContainer = colors.copy(containerColor = OTHER_CONTAINER)
        val newContent = colors.copy(contentColor = OTHER_CONTENT)

        // Assert
        assertEquals(
            expected = ScreenProblemMessageColors(containerColor = OTHER_CONTAINER, contentColor = CONTENT),
            actual = newContainer,
        )
        assertEquals(
            expected = ScreenProblemMessageColors(containerColor = CONTAINER, contentColor = OTHER_CONTENT),
            actual = newContent,
        )
        assertNotEquals(illegal = colors, actual = newContainer)
    }
}
