package dev.tonholo.ssrcompose.contracttests.textgate

import kotlin.test.Test
import kotlin.test.assertEquals

class Sha256HexTest {
    @Test
    fun `given the bytes of abc, when hashed, then the hex is the FIPS 180-2 test value`() {
        // Arrange
        val bytes = "abc".encodeToByteArray()

        // Act
        val hex = Sha256Hex.of(bytes)

        // Assert
        assertEquals(expected = FIPS_ABC_DIGEST, actual = hex)
    }

    private companion object {
        const val FIPS_ABC_DIGEST = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
    }
}
