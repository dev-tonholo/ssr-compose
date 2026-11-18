package dev.tonholo.ssrcompose.catalog.index.binary

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException

/**
 * The strings of an index. Each one is decoded once, so a file that refers to one long string many times cannot make
 * the reader hold that text many times.
 */
internal class StringTable private constructor(
    private val bytes: ByteArray,
    private val offsets: IntArray,
    private val start: Int,
) {
    private val decoded = arrayOfNulls<String>(size = offsets.size - 1)

    operator fun get(id: Int): String {
        require(id in decoded.indices) { DAMAGED_INDEX }
        return decoded[id] ?: decode(id = id).also { text -> decoded[id] = text }
    }

    private fun decode(id: Int): String {
        val encoded = ByteBuffer.wrap(bytes, start + offsets[id], offsets[id + 1] - offsets[id])
        return try {
            Charsets.UTF_8.newDecoder().decode(encoded).toString()
        } catch (ignored: CharacterCodingException) {
            throw IllegalArgumentException(DAMAGED_INDEX, ignored)
        }
    }

    companion object {
        fun read(cursor: BinaryCursor, bytes: ByteArray): StringTable {
            val count = cursor.count(minimumElementBytes = Int.SIZE_BYTES)
            val offsets = IntArray(size = count + 1) { cursor.int() }
            val inOrder = (1..count).all { index -> offsets[index - 1] <= offsets[index] }
            require(offsets.first() == 0 && inOrder) { DAMAGED_INDEX }
            val start = cursor.position
            cursor.skip(byteCount = offsets.last())
            return StringTable(bytes = bytes, offsets = offsets, start = start)
        }
    }
}
