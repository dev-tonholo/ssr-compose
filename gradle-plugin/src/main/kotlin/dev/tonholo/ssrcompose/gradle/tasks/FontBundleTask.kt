package dev.tonholo.ssrcompose.gradle.tasks

import dev.tonholo.ssrcompose.tooling.font.FontBundleStep
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/** `ssrFontBundle`: writes the app's font bundle, `fonts.json` and the font files, from the declared fonts. */
@CacheableTask
internal abstract class FontBundleTask : DefaultTask() {
    @get:Input
    abstract val version: Property<String>

    @get:Nested
    abstract val fonts: ListProperty<FontInput>

    @get:OutputDirectory
    abstract val bundleFolder: DirectoryProperty

    @TaskAction
    fun bundle() = runStep {
        FontBundleStep.run(
            version = version.get(),
            declarations = fonts.get().map { font -> font.toDeclaration() },
            bundleFolder = bundleFolder.get().asFile,
        )
    }
}
