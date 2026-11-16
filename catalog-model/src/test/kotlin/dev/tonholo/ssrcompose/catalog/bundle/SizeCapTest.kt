package dev.tonholo.ssrcompose.catalog.bundle

import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SizeCapTest {
    @Test
    fun `given a negative number of bytes, when a size cap is created, then it is rejected`() {
        // Arrange
        val bytes = -1

        // Act
        val create = { SizeCap(bytes = bytes) }

        // Assert
        assertFailsWith<IllegalArgumentException> { create() }
    }

    @Test
    fun `given a stream of exactly the cap, when it is read, then all its bytes are given`() {
        // Arrange
        val content = ByteArray(size = CAP) { BYTE }

        // Act
        val actual = SizeCap(bytes = CAP).read(stream = ByteArrayInputStream(content), subject = APP_PART_SUBJECT)

        // Assert
        assertContentEquals(expected = content, actual = actual)
    }

    @Test
    fun `given a stream one byte over the cap, when it is read, then it is refused naming the subject`() {
        // Arrange
        val stream = ByteArrayInputStream(ByteArray(size = CAP + 1) { BYTE })

        // Act
        val read = { SizeCap(bytes = CAP).read(stream = stream, subject = APP_PART_SUBJECT) }

        // Assert
        val message = assertFailsWith<CatalogFileException> { read() }.message.orEmpty()
        assertTrue(
            actual = message.contains(other = "The $APP_PART_SUBJECT is larger than $CAP bytes"),
            message = message,
        )
    }

    @Test
    fun `given a stream that never ends, when it is read, then it is refused after the cap and one byte`() {
        // Arrange
        val stream = EndlessStream()

        // Act
        val read = { SizeCap(bytes = CAP).read(stream = stream, subject = APP_PART_SUBJECT) }

        // Assert
        assertFailsWith<CatalogFileException> { read() }
        assertTrue(actual = stream.handedOut <= CAP + 1, message = "${stream.handedOut} bytes were read")
    }

    @Test
    fun `given the largest cap, when a small stream is read, then all its bytes are given`() {
        // Arrange
        val content = ByteArray(size = CAP) { BYTE }

        // Act
        val actual = SizeCap(bytes = Int.MAX_VALUE).read(
            stream = ByteArrayInputStream(content),
            subject = APP_PART_SUBJECT,
        )

        // Assert
        assertEquals(expected = CAP, actual = actual.size)
    }

    private companion object {
        const val CAP = 1_000
        const val BYTE: Byte = 120
    }
}
