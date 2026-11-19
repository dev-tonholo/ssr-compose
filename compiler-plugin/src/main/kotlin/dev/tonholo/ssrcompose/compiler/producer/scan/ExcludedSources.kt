package dev.tonholo.ssrcompose.compiler.producer.scan

import dev.tonholo.ssrcompose.catalog.options.SourceFileList
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths

/** Reads which files of the source folders the compilation does not receive. */
internal object ExcludedSources {
    /**
     * Returns the files in [listFile], without any file under [compilerRoots], because the compiler receives those
     * whatever an old list says. Nothing is excluded when the build has not written the list yet.
     */
    fun read(listFile: File?, compilerRoots: List<File>): Set<Path> {
        val received = compilerRoots.mapTo(destination = HashSet()) { root -> root.path.normalizedPath() }
        return listedIn(listFile = listFile).filterNotTo(destination = HashSet()) { path ->
            generateSequence(seed = path) { it.parent }.any { ancestor -> ancestor in received }
        }
    }

    private fun listedIn(listFile: File?): List<Path> {
        if (listFile == null) return emptyList()
        return try {
            SourceFileList.decode(text = listFile.readText()).map { path -> path.normalizedPath() }
        } catch (_: IOException) {
            emptyList()
        }
    }

    private fun String.normalizedPath(): Path = Paths.get(this).toAbsolutePath().normalize()
}
