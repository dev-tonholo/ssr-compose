package dev.tonholo.ssrcompose.producer

import dev.tonholo.ssrcompose.producer.testing.callableOverloads
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val BASIC_TEXT_FACADE = "androidx.compose.foundation.text.BasicTextKt"
private const val SCALED_BASIC_TEXT_FACADE = "dev.tonholo.ssrcompose.producer.ScaledBasicTextKt"
private const val BASIC_TEXT = "BasicText"
private const val SCALED_BASIC_TEXT = "ScaledBasicText"

/**
 * The expected overloads are the ones foundation's own compiled `BasicText` declares. Kotlin metadata says whether a
 * parameter has a default, and not what the default is, so a changed default value is not seen here.
 */
class ScaledBasicTextOverloadsTest {
    @Test
    fun `given the callable BasicText overloads, when ScaledBasicText is compared, then the parameters are the same`() {
        // Arrange
        val expected = callableOverloads(facadeClassName = BASIC_TEXT_FACADE, functionName = BASIC_TEXT)

        // Act
        val actual = callableOverloads(facadeClassName = SCALED_BASIC_TEXT_FACADE, functionName = SCALED_BASIC_TEXT)

        // Assert
        assertTrue(actual = expected.isNotEmpty(), message = "foundation declares no callable BasicText")
        assertEquals(expected = expected.toSet(), actual = actual.toSet())
        assertEquals(expected = expected.size, actual = actual.size)
    }
}
