package dev.tonholo.ssrcompose.gradle

import dev.tonholo.ssrcompose.catalog.options.SourceFileList
import org.gradle.api.Action
import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider

/**
 * Lists the Kotlin files of [sourceFolders] that [compiledSources] leaves out, so the compiler plugin does not create
 * a bridge for a name the compile task does not track.
 */
internal class ExcludedSourcesWriter(
    private val sourceFolders: FileCollection,
    private val compiledSources: FileCollection,
    private val listFile: Provider<RegularFile>,
    private val isNeeded: Provider<Boolean>,
) : Action<Task> {
    override fun execute(task: Task) {
        if (!isNeeded.get()) return
        val compiled = compiledSources.files.mapTo(destination = HashSet()) { file -> file.absolutePath }
        val excluded = sourceFolders.asFileTree
            .matching { patterns -> patterns.include(KOTLIN_FILES) }
            .files
            .map { file -> file.absolutePath }
            .filterNot { path -> path in compiled }
        val file = listFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(text = SourceFileList.encode(paths = excluded))
    }

    private companion object {
        val KOTLIN_FILES = listOf("**/*.kt", "**/*.kts")
    }
}
