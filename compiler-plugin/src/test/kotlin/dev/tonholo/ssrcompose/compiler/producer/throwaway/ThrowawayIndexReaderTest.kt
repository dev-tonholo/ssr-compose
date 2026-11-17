package dev.tonholo.ssrcompose.compiler.producer.throwaway

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ThrowawayIndexReaderTest {
    @Test
    fun `given an optional action-of-text, when the index is parsed, then it is refused and names the parameter`() {
        // Arrange
        val lines = listOf(
            "component Greeting",
            "param onCancel action-of-text optional",
        )

        // Act
        val failure = assertFailsWith<IllegalArgumentException> { ThrowawayIndexReader.parse(lines = lines) }

        // Assert
        assertTrue(actual = failure.message.orEmpty().contains("onCancel"), message = failure.message)
        assertTrue(actual = failure.message.orEmpty().contains("optional"), message = failure.message)
    }

    @Test
    fun `given an optional slot-of-text, when the index is parsed, then it is refused and names the parameter`() {
        // Arrange
        val lines = listOf(
            "component PaymentSection",
            "param content slot-of-text optional",
        )

        // Act
        val failure = assertFailsWith<IllegalArgumentException> { ThrowawayIndexReader.parse(lines = lines) }

        // Assert
        assertTrue(actual = failure.message.orEmpty().contains("content"), message = failure.message)
        assertTrue(actual = failure.message.orEmpty().contains(CANNOT_BE_OPTIONAL), message = failure.message)
    }

    private companion object {
        const val CANNOT_BE_OPTIONAL = "cannot be optional"
    }
}
