package dev.tonholo.ssrcompose.gradle

import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation

/** Where a compilation lists the files of its source folders that it does not receive. */
internal object ExcludedSourcesListFile {
    private const val LIST_FOLDER = "ssr/excluded-sources"

    fun fileOf(project: Project, compilation: KotlinCompilation<*>): Provider<RegularFile> =
        project.layout.buildDirectory.file("$LIST_FOLDER/${compilation.target.targetName}/${compilation.name}.list")
}
