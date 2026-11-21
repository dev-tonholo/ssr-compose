package dev.tonholo.ssrcompose.gradle

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.android.build.api.variant.SourceDirectories
import dev.tonholo.ssrcompose.gradle.tasks.FontVersionTask
import dev.tonholo.ssrcompose.tooling.font.FontStepNames
import org.gradle.api.Project

/**
 * Adds the font version resource to each Android variant of the module that declares the fonts (ADR 0022). Every
 * Android type of the plugin is in this file, reached only once an Android plugin is applied.
 */
internal class AndroidFontVersions(private val project: Project, private val extension: SsrComposeExtension) {
    fun registerWhenAndroid() {
        project.pluginManager.withPlugin(APPLICATION_PLUGIN) {
            val components = project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
            components.onVariants(components.selector().all()) { variant ->
                addFontVersion(variantName = variant.name, resources = variant.sources.resources)
            }
        }
        project.pluginManager.withPlugin(LIBRARY_PLUGIN) {
            val components = project.extensions.getByType(LibraryAndroidComponentsExtension::class.java)
            components.onVariants(components.selector().all()) { variant ->
                addFontVersion(variantName = variant.name, resources = variant.sources.resources)
            }
        }
        project.pluginManager.withPlugin(MULTIPLATFORM_LIBRARY_PLUGIN) {
            val components = project.extensions.getByType(KotlinMultiplatformAndroidComponentsExtension::class.java)
            components.onVariants(components.selector().all()) { variant ->
                addFontVersion(variantName = variant.name, resources = variant.sources.resources)
            }
        }
    }

    private fun addFontVersion(variantName: String, resources: SourceDirectories.Flat?) {
        val settings = extension.fontSettings
        if (!extension.isConsumerConfigured() || !settings.hasFonts) return
        val taskName = FontStepNames.VERSION + variantName.replaceFirstChar { it.uppercase() }
        val task = project.tasks.register(taskName, FontVersionTask::class.java) { task ->
            task.group = GROUP
            task.description = DESCRIPTION
            task.version.set(settings.version.orElse(""))
        }
        resources?.addGeneratedSourceDirectory(task, FontVersionTask::resourceFolder)
    }

    private companion object {
        const val APPLICATION_PLUGIN = "com.android.application"
        const val LIBRARY_PLUGIN = "com.android.library"
        const val MULTIPLATFORM_LIBRARY_PLUGIN = "com.android.kotlin.multiplatform.library"
        const val GROUP = "ssr"
        const val DESCRIPTION = "Writes the font version resource the consumer reads."
    }
}
