package dev.tonholo.ssrcompose.catalog.model

import kotlin.test.Test
import kotlin.test.assertEquals

class PartNameTest {
    @Test
    fun `given lowercase letters, digits and hyphens, when a part name is created, then it is accepted`() {
        // Arrange
        val names = listOf("app", "feature-home", "feature-home-2", "0", "a".repeat(n = LONGEST_NAME))

        // Act
        val refused = names.filter { name -> runCatching { PartName(value = name) }.isFailure }

        // Assert
        assertEquals(expected = emptyList(), actual = refused)
    }

    @Test
    fun `given an empty name or one over 64 characters, when a part name is created, then it is rejected`() {
        // Arrange
        val names = listOf("", "a".repeat(n = LONGEST_NAME + 1))

        // Act
        val accepted = names.filter { name -> runCatching { PartName(value = name) }.isSuccess }

        // Assert
        assertEquals(expected = emptyList(), actual = accepted)
    }

    @Test
    fun `given a character that is not allowed, when a part name is created, then it is rejected`() {
        // Arrange
        val names = listOf("Feature", "feature_home", "feature.home", "..", "a/b", "a\\b", "a:b", "a b", "café")

        // Act
        val accepted = names.filter { name -> runCatching { PartName(value = name) }.isSuccess }

        // Assert
        assertEquals(expected = emptyList(), actual = accepted)
    }

    @Test
    fun `given a name Windows keeps for a device, when a part name is created, then it is rejected`() {
        // Arrange
        val ports = (1..LAST_DEVICE_PORT).flatMap { port -> listOf("com$port", "lpt$port") }
        val names = listOf("con", "prn", "aux", "nul") + ports

        // Act
        val accepted = names.filter { name -> runCatching { PartName(value = name) }.isSuccess }

        // Assert
        assertEquals(expected = emptyList(), actual = accepted)
    }

    @Test
    fun `given a name that only starts like a device name, when a part name is created, then it is accepted`() {
        // Arrange
        val names = listOf("console", "com10", "con-1", "nul2")

        // Act
        val refused = names.filter { name -> runCatching { PartName(value = name) }.isFailure }

        // Assert
        assertEquals(expected = emptyList(), actual = refused)
    }

    private companion object {
        const val LONGEST_NAME = 64
        const val LAST_DEVICE_PORT = 9
    }
}
