package dev.tonholo.ssrcompose.catalog.bundle.fingerprint

import kotlin.test.Test
import kotlin.test.assertEquals

class FingerprintTest {
    @Test
    fun `given abc, when the fingerprint is computed, then it is the known SHA-256 in lowercase hexadecimal`() {
        // Arrange
        val bytes = "abc".encodeToByteArray()

        // Act
        val actual = Fingerprint.of(bytes = bytes)

        // Assert
        assertEquals(expected = SHA256_ABC, actual = actual)
    }

    private companion object {
        const val SHA256_ABC = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
    }
}
