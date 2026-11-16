package dev.tonholo.ssrcompose.catalog.options

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CatalogOptionValueTest {
    @Test
    fun `given a field with the separator, when it is encoded and decoded, then it is the same value`() {
        // Arrange
        val value = CatalogOptionValue(
            version = "a|b",
            indexPath = "/build/odd|folder/|3.index|",
            bridgePackage = "com.example.app",
        )

        // Act
        val decoded = CatalogOptionValue.decode(value.encode())

        // Assert
        assertEquals(expected = value, actual = decoded)
    }

    @Test
    fun `given a path with backslashes, when it is encoded and decoded, then it is the same value`() {
        // Arrange
        val value = CatalogOptionValue(
            version = "3",
            indexPath = """C:\build\ssr\index\3.index""",
            bridgePackage = "com.example.app",
        )

        // Act
        val decoded = CatalogOptionValue.decode(value.encode())

        // Assert
        assertEquals(expected = value, actual = decoded)
    }

    @Test
    fun `given a text with two fields, when it is decoded, then it fails and names the catalog option`() {
        // Arrange
        val text = "3|/build/3.index"

        // Act
        val message = decodeFailureMessage(text)

        // Assert
        assertTrue(actual = message.contains(CompilerPluginOptions.CATALOG), message = message)
    }

    @Test
    fun `given a text with four fields, when it is decoded, then it fails`() {
        // Arrange
        val text = "3|/build/3.index|com.example|extra"

        // Act
        val result = runCatching { CatalogOptionValue.decode(text) }

        // Assert
        assertTrue(actual = result.exceptionOrNull() is IllegalArgumentException, message = result.toString())
    }

    @Test
    fun `given a text with an empty field, when it is decoded, then it fails`() {
        // Arrange
        val text = "3||com.example"

        // Act
        val result = runCatching { CatalogOptionValue.decode(text) }

        // Assert
        assertTrue(actual = result.exceptionOrNull() is IllegalArgumentException, message = result.toString())
    }

    @Test
    fun `given a text ending in a lone backslash, when it is decoded, then it fails and names the catalog option`() {
        // Arrange
        val text = """3|/build/3.index|com.example\"""

        // Act
        val message = decodeFailureMessage(text)

        // Assert
        assertTrue(actual = message.contains(CompilerPluginOptions.CATALOG), message = message)
    }

    @Test
    fun `given a text with an unknown escape, when it is decoded, then it fails`() {
        // Arrange
        val text = """3|/build/\x.index|com.example"""

        // Act
        val result = runCatching { CatalogOptionValue.decode(text) }

        // Assert
        assertTrue(actual = result.exceptionOrNull() is IllegalArgumentException, message = result.toString())
    }

    private fun decodeFailureMessage(text: String): String =
        assertFailsWith<IllegalArgumentException> { CatalogOptionValue.decode(text) }.message.orEmpty()
}
