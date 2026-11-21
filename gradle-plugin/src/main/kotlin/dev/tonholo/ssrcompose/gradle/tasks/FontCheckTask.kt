package dev.tonholo.ssrcompose.gradle.tasks

import dev.tonholo.ssrcompose.tooling.font.FontCheck
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

/** `ssrFontCheck`: compares the font bundle with the saved one of its version, byte for byte. */
@UntrackedTask(because = "It compares with the committed ssr/fonts folder and writes nothing.")
internal abstract class FontCheckTask : DefaultTask() {
    @get:Input
    abstract val version: Property<String>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val bundleFolder: DirectoryProperty

    @get:Internal
    abstract val savedRoot: DirectoryProperty

    @TaskAction
    fun check() = runStep {
        FontCheck.run(
            bundleFolder = bundleFolder.get().asFile,
            savedRoot = savedRoot.get().asFile,
            version = version.get(),
        )
    }
}
