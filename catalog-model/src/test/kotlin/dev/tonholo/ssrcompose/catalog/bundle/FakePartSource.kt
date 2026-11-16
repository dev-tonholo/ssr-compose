package dev.tonholo.ssrcompose.catalog.bundle

/**
 * Gives the bytes of part files by name, and remembers which names were asked for, in order.
 */
internal class FakePartSource(private val files: Map<String, ByteArray>) {
    val requested: MutableList<String> = mutableListOf()

    fun read(fileName: String): ByteArray {
        requested += fileName
        return files.getValue(key = fileName)
    }
}
