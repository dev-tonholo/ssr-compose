package dev.tonholo.ssrcompose.core.font

import kotlin.test.Test
import kotlin.test.assertEquals

/** The resource that carries the app's font version from the build to the consumer. */
class FontVersionResourceTest {
    @Test
    fun `given a font version, when it is written, then the bytes are its label and nothing else`() {
        // Arrange
        val version = FontBundleVersion(value = LABEL)

        // Act
        val actual = FontVersionResource.write(version = version).decodeToString()

        // Assert
        assertEquals(expected = LABEL, actual = actual)
    }

    @Test
    fun `given written bytes, when they are read, then they give the same version`() {
        // Arrange
        val version = FontBundleVersion(value = LABEL)
        val bytes = FontVersionResource.write(version = version)

        // Act
        val actual = FontVersionResource.read(bytes = bytes)

        // Assert
        assertEquals(expected = version, actual = actual)
    }

    @Test
    fun `given bytes that are not a font version label, when read, then only a font bundle error is thrown`() {
        // Arrange
        val contents = listOf("", "A", "../1", "1\n", "v".repeat(n = MAX_LENGTH + 1)).map { it.encodeToByteArray() }

        // Act
        val actual = contents.map { bytes ->
            runCatching { FontVersionResource.read(bytes = bytes) }.exceptionOrNull()?.let { failure -> failure::class }
        }

        // Assert
        assertEquals(expected = contents.map { FontBundleException::class }, actual = actual)
    }

    private companion object {
        const val LABEL = "brand-2.0_a"
        const val MAX_LENGTH = 64
    }
}
