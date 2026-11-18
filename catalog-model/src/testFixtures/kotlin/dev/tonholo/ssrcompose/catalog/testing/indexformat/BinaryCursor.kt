package dev.tonholo.ssrcompose.catalog.testing.indexformat

import java.nio.ByteBuffer

internal const val NOT_A_BINARY_INDEX = "This file is not a binary index"

/**
 * Reads a binary index and refuses anything the file does not hold.
 *
 * @throws IllegalArgumentException from every read that would go past the end of the file.
 */
internal class BinaryCursor(bytes: ByteArray, start: Int = 0) {
    private val buffer = ByteBuffer.wrap(bytes).position(start)

    val position: Int
        get() = buffer.position()

    val remaining: Int
        get() = buffer.remaining()

    fun byte(): Int {
        require(remaining >= Byte.SIZE_BYTES) { NOT_A_BINARY_INDEX }
        return buffer.get().toInt()
    }

    fun int(): Int {
        require(remaining >= Int.SIZE_BYTES) { NOT_A_BINARY_INDEX }
        return buffer.getInt()
    }

    /** Reads how many elements follow, each of at least [minimumElementBytes], and checks that they can fit. */
    fun count(minimumElementBytes: Int): Int {
        val count = int()
        require(count >= 0 && count.toLong() * minimumElementBytes <= remaining) { NOT_A_BINARY_INDEX }
        return count
    }

    fun skip(byteCount: Int) {
        require(byteCount in 0..remaining) { NOT_A_BINARY_INDEX }
        buffer.position(position + byteCount)
    }
}
