package dev.tonholo.ssrcompose.tooling.fingerprint

import dev.tonholo.ssrcompose.tooling.StepException
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

/** A fetched or saved file is accepted only when its SHA-256 matches the fingerprint it is pinned to. */
class FingerprintCheckTest {
    private fun check(bytes: ByteArray) =
        runCatching { FingerprintCheck.require(bytes = bytes, fingerprint = ABC_SHA256, subject = SUBJECT) }

    @Test
    fun `given bytes whose SHA-256 is the fingerprint, when checked, then they are accepted`() {
        // Arrange
        val bytes = ABC.encodeToByteArray()

        // Act
        val actual = check(bytes = bytes)

        // Assert
        assertEquals(expected = null, actual = actual.exceptionOrNull())
    }

    @Test
    fun `given bytes whose SHA-256 differs, when checked, then they are refused naming the file`() {
        // Arrange
        val bytes = "abd".encodeToByteArray()

        // Act
        val actual = check(bytes = bytes)

        // Assert
        val failure = actual.exceptionOrNull()
        assertEquals(expected = StepException::class, actual = failure?.let { it::class })
        assertContains(charSequence = failure?.message.orEmpty(), other = SUBJECT)
    }

    private companion object {
        const val SUBJECT = "font file brand/400-normal.ttf"
        const val ABC = "abc"

        /** The SHA-256 of "abc", from FIPS 180-2, appendix B.1. */
        const val ABC_SHA256 = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
    }
}
