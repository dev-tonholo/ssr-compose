package dev.tonholo.ssrcompose.catalog.index.binary

import java.nio.ByteBuffer

internal const val DAMAGED_INDEX = "the file is damaged or is not a catalog index"

/**
 * Reads the part of a binary index from [start] up to [end], and refuses anything that part does not hold.
 *
 * @throws IllegalArgumentException from every read that would go past [end].
 */
internal class BinaryCursor(bytes: ByteArray, start: Int = 0, end: Int = bytes.size) {
    private val buffer = ByteBuffer.wrap(bytes, start, end - start)

    val position: Int
        get() = buffer.position()

    val remaining: Int
        get() = buffer.remaining()

    fun byte(): Int {
        require(remaining >= Byte.SIZE_BYTES) { DAMAGED_INDEX }
        return buffer.get().toInt()
    }

    fun int(): Int {
        require(remaining >= Int.SIZE_BYTES) { DAMAGED_INDEX }
        return buffer.getInt()
    }

    /** Reads how many elements follow, each of at least [minimumElementBytes], and checks that they can fit. */
    fun count(minimumElementBytes: Int): Int {
        val count = int()
        require(count >= 0 && count.toLong() * minimumElementBytes <= remaining) { DAMAGED_INDEX }
        return count
    }

    fun skip(byteCount: Int) {
        require(byteCount in 0..remaining) { DAMAGED_INDEX }
        buffer.position(position + byteCount)
    }
}
