package dev.tonholo.ssrcompose.gradle.tasks

import dev.tonholo.ssrcompose.tooling.font.FontDump
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

/** `ssrFontDump`: saves the font bundle to `ssr/fonts/<version>/`. Run on purpose, after raising the version. */
@UntrackedTask(because = "It writes the committed ssr/fonts folder.")
internal abstract class FontDumpTask : DefaultTask() {
    @get:Input
    abstract val version: Property<String>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val bundleFolder: DirectoryProperty

    @get:Internal
    abstract val savedRoot: DirectoryProperty

    @TaskAction
    fun dump() = runStep {
        FontDump.run(
            bundleFolder = bundleFolder.get().asFile,
            savedRoot = savedRoot.get().asFile,
            version = version.get(),
        )
    }
}
