package dev.tonholo.ssrcompose.gradle

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider

/** Where the index of a held catalog is written. */
internal object CatalogIndexes {
    private const val INDEX_FOLDER = "ssr/index"
    private const val INDEX_EXTENSION = "index"

    fun fileOf(project: Project, version: String): Provider<RegularFile> =
        project.layout.buildDirectory.file("$INDEX_FOLDER/$version.$INDEX_EXTENSION")
}
