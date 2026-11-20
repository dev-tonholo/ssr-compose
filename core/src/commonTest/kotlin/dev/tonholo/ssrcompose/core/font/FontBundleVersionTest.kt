package dev.tonholo.ssrcompose.core.font

import dev.tonholo.ssrcompose.core.message.MessageDecodingException
import dev.tonholo.ssrcompose.core.message.internal.JsonMessageCodec
import kotlin.test.Test
import kotlin.test.assertEquals

class FontBundleVersionTest {
    private fun rejected(labels: List<String>): List<String> = labels.filter { label ->
        runCatching { FontBundleVersion(value = label) }.exceptionOrNull() !is IllegalArgumentException
    }

    @Test
    fun `given labels that break the folder label rule, when a FontBundleVersion is created, then each is rejected`() {
        // Arrange
        val labels = listOf("", "v".repeat(n = MAX_LENGTH + 1), "A", ".1", "1.", "a/b", "a b", "con", "con.x", "lpt9")

        // Act
        val notRejected = rejected(labels = labels)

        // Assert
        assertEquals(expected = emptyList(), actual = notRejected)
    }

    @Test
    fun `given labels the rule allows, when a FontBundleVersion is created, then each is kept`() {
        // Arrange
        val labels = listOf("1", "brand-2.0_a", "v".repeat(n = MAX_LENGTH), "console", "com10")

        // Act
        val actual = labels.map { label -> FontBundleVersion(value = label).value }

        // Assert
        assertEquals(expected = labels, actual = actual)
    }

    @Test
    fun `given a FontVersionChanged with a version that breaks the rule, when decoded, then it is refused`() {
        // Arrange
        // language=json
        val frame = """{"type":"FontVersionChanged","fontVersion":"../1"}""".encodeToByteArray()

        // Act
        val actual = runCatching { JsonMessageCodec().decodeConsumerMessage(frame = frame) }
            .exceptionOrNull()?.let { failure -> failure::class }

        // Assert
        assertEquals(expected = MessageDecodingException::class, actual = actual)
    }

    private companion object {
        const val MAX_LENGTH = 64
    }
}
