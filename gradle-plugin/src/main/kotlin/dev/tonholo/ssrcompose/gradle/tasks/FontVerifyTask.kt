package dev.tonholo.ssrcompose.gradle.tasks

import dev.tonholo.ssrcompose.tooling.font.FontVerify
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

/** `ssrFontVerify`: checks the saved font bundles against `ssr/fonts.lock`. */
@UntrackedTask(because = "It checks the committed ssr/fonts folder and writes nothing.")
internal abstract class FontVerifyTask : DefaultTask() {
    @get:Input
    abstract val versions: ListProperty<String>

    @get:Internal
    abstract val savedRoot: DirectoryProperty

    @get:Internal
    abstract val lockFile: RegularFileProperty

    @TaskAction
    fun verify() = runStep {
        FontVerify.run(versions = versions.get(), savedRoot = savedRoot.get().asFile, lockFile = lockFile.get().asFile)
    }
}
