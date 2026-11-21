package dev.tonholo.ssrcompose.gradle

import dev.tonholo.ssrcompose.gradle.tasks.FontBundleTask
import dev.tonholo.ssrcompose.gradle.tasks.FontCheckTask
import dev.tonholo.ssrcompose.gradle.tasks.FontDumpTask
import dev.tonholo.ssrcompose.gradle.tasks.FontInput
import dev.tonholo.ssrcompose.tooling.font.FontStepNames
import org.gradle.api.Project
import org.gradle.api.Task

/** Registers the app's font steps. With no declared font, each is skipped. */
internal object AppFontTasks {
    private const val BUNDLE_FOLDER = "ssr/fonts/bundle"
    private const val NO_FONT_REASON = "no font family is declared in ssrCompose { fonts { } }"

    fun register(project: Project, settings: FontSettings) {
        val hasFonts = settings.families.any { family -> family.fonts.isNotEmpty() }
        val version = settings.version.orElse("")
        val savedRoot = project.layout.projectDirectory.dir(SAVED_FONTS_FOLDER)
        val bundle = project.tasks.register(FontStepNames.BUNDLE, FontBundleTask::class.java) { task ->
            task.describe(text = "Writes the app's font bundle from the declared fonts.", hasFonts = hasFonts)
            task.version.set(version)
            task.fonts.set(fontInputs(project = project, settings = settings))
            task.bundleFolder.set(project.layout.buildDirectory.dir(BUNDLE_FOLDER))
        }
        val bundleFolder = bundle.flatMap { task -> task.bundleFolder }
        val check = project.tasks.register(FontStepNames.CHECK, FontCheckTask::class.java) { task ->
            task.describe(text = "Compares the font bundle with the saved one of its version.", hasFonts = hasFonts)
            task.version.set(version)
            task.bundleFolder.set(bundleFolder)
            task.savedRoot.set(savedRoot)
        }
        project.tasks.register(FontStepNames.DUMP, FontDumpTask::class.java) { task ->
            task.describe(text = "Saves the font bundle to ssr/fonts/<version>/.", hasFonts = hasFonts)
            task.version.set(version)
            task.bundleFolder.set(bundleFolder)
            task.savedRoot.set(savedRoot)
        }
        project.addToCheck(task = check)
    }

    private fun fontInputs(project: Project, settings: FontSettings): List<FontInput> =
        settings.families.flatMap { family ->
            family.fonts.map { font ->
                project.objects.newInstance(FontInput::class.java).apply {
                    this.family.set(family.name)
                    weight.set(font.weight)
                    italic.set(font.italic)
                    file.set(project.layout.projectDirectory.file(font.file))
                }
            }
        }

    private fun Task.describe(text: String, hasFonts: Boolean) =
        describeFontStep(text = text, hasWork = hasFonts, skipReason = NO_FONT_REASON)
}
