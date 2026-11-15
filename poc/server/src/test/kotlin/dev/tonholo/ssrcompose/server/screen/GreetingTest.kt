package dev.tonholo.ssrcompose.server.screen

import kotlin.test.Test
import kotlin.test.assertEquals

class GreetingTest {
    @Test
    fun `given hour 0, when greetingFor is called, then it returns Good morning`() {
        // Arrange
        val hour = 0

        // Act
        val actual = greetingFor(hour = hour)

        // Assert
        assertEquals(expected = "Good morning", actual = actual)
    }

    @Test
    fun `given hour 11, when greetingFor is called, then it returns Good morning`() {
        // Arrange
        val hour = 11

        // Act
        val actual = greetingFor(hour = hour)

        // Assert
        assertEquals(expected = "Good morning", actual = actual)
    }

    @Test
    fun `given hour 12, when greetingFor is called, then it returns Good afternoon`() {
        // Arrange
        val hour = 12

        // Act
        val actual = greetingFor(hour = hour)

        // Assert
        assertEquals(expected = "Good afternoon", actual = actual)
    }

    @Test
    fun `given hour 17, when greetingFor is called, then it returns Good afternoon`() {
        // Arrange
        val hour = 17

        // Act
        val actual = greetingFor(hour = hour)

        // Assert
        assertEquals(expected = "Good afternoon", actual = actual)
    }

    @Test
    fun `given hour 18, when greetingFor is called, then it returns Good evening`() {
        // Arrange
        val hour = 18

        // Act
        val actual = greetingFor(hour = hour)

        // Assert
        assertEquals(expected = "Good evening", actual = actual)
    }

    @Test
    fun `given hour 23, when greetingFor is called, then it returns Good evening`() {
        // Arrange
        val hour = 23

        // Act
        val actual = greetingFor(hour = hour)

        // Assert
        assertEquals(expected = "Good evening", actual = actual)
    }
}
