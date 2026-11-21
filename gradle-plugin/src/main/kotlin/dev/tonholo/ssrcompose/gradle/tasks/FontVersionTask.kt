package dev.tonholo.ssrcompose.gradle.tasks

import dev.tonholo.ssrcompose.tooling.font.FontVersionResourceStep
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/** `ssrFontVersion<Variant>`: writes the resource that carries the app's font version, for one Android variant. */
@CacheableTask
internal abstract class FontVersionTask : DefaultTask() {
    @get:Input
    abstract val version: Property<String>

    /** Set by the Android Gradle plugin to the variant's generated resources folder. */
    @get:OutputDirectory
    abstract val resourceFolder: DirectoryProperty

    @TaskAction
    fun write() = runStep {
        FontVersionResourceStep.run(version = version.get(), resourceFolder = resourceFolder.get().asFile)
    }
}
