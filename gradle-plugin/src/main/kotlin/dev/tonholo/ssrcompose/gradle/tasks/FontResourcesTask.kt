package dev.tonholo.ssrcompose.gradle.tasks

import dev.tonholo.ssrcompose.tooling.font.FontResourcesStep
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** `ssrFontResources`: puts the saved font bundles in the server's resources, where the producer loads them. */
@CacheableTask
internal abstract class FontResourcesTask : DefaultTask() {
    @get:Input
    abstract val versions: ListProperty<String>

    /** The saved bundles, tracked by their path inside the saved folder only. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val savedBundles: ConfigurableFileCollection

    /** The lock file the saved bundles are checked against, tracked by content only. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val lockFiles: ConfigurableFileCollection

    @get:Internal
    abstract val savedRoot: DirectoryProperty

    @get:Internal
    abstract val lockFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputFolder: DirectoryProperty

    @TaskAction
    fun copy() = runStep {
        FontResourcesStep.run(
            versions = versions.get(),
            savedRoot = savedRoot.get().asFile,
            lockFile = lockFile.get().asFile,
            outputFolder = outputFolder.get().asFile,
        )
    }
}
