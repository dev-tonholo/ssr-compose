package dev.tonholo.ssrcompose.compiler.producer.scan

import java.io.File

/** Chooses what a scan reads, so a file the compiler receives inside a source folder is read only once. */
internal object ScanRoots {
    fun of(sourceFolders: List<File>, compilerRoots: List<File>): List<File> {
        val folders = sourceFolders.map { folder -> folder.normalizedAbsolute() }.distinct()
        val outside = compilerRoots.map { root -> root.normalizedAbsolute() }.distinct()
            .filterNot { root -> folders.any { folder -> root.startsWith(folder) } }
        return folders + outside
    }

    private fun File.normalizedAbsolute(): File = toPath().toAbsolutePath().normalize().toFile()
}
