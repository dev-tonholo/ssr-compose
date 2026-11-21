package dev.tonholo.ssrcompose.tooling.compare

import java.io.File
import java.io.InputStream

private const val BUFFER_SIZE = 64 * 1024

/** Compares two folders file by file, byte for byte. */
public object FolderComparison {
    /**
     * Names each file that differs between [expected] and [actual]: changed, missing from [actual], or only in
     * [actual]. A folder that does not exist counts as empty.
     *
     * @return the relative paths, with `/` between segments, sorted.
     */
    public fun differingFiles(expected: File, actual: File): List<String> {
        val expectedFiles = relativeFiles(folder = expected)
        val actualFiles = relativeFiles(folder = actual)
        return (expectedFiles + actualFiles).sorted().filter { path ->
            path !in expectedFiles || path !in actualFiles ||
                !sameBytes(first = expected.resolve(path), second = actual.resolve(path))
        }
    }

    private fun relativeFiles(folder: File): Set<String> =
        folder.walkTopDown().filter { it.isFile }.map { it.relativeTo(folder).invariantSeparatorsPath }.toSet()

    private fun sameBytes(first: File, second: File): Boolean =
        first.length() == second.length() &&
            first.inputStream().use { a -> second.inputStream().use { b -> sameContent(first = a, second = b) } }

    private fun sameContent(first: InputStream, second: InputStream): Boolean {
        while (true) {
            val a = first.readNBytes(BUFFER_SIZE)
            val b = second.readNBytes(BUFFER_SIZE)
            if (!a.contentEquals(b)) return false
            if (a.isEmpty()) return true
        }
    }
}
