package dev.tonholo.ssrcompose.conventions

import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer
import java.io.File

private const val DETEKT_CONFIG_PATH = "config/detekt/detekt.yml"
private const val SOURCE_FOLDER_NAME = "src"
private const val SOURCE_SEGMENT_INDEX = 0
private const val RESOURCES_FOLDER_NAME = "resources"
private const val RESOURCES_SEGMENT_INDEX = 2

/**
 * Applies detekt with the one shared config file at `config/detekt/detekt.yml` and makes the `detekt` task read every
 * Kotlin source set of the module, including Kotlin Multiplatform and extra test source sets. Generated sources under
 * the build folder and files under `src/<sourceSet>/resources` are left out, because test resources are compiler input.
 *
 * Suppressed unused: Gradle creates this plugin from the declaration in `build-logic/build.gradle.kts`.
 */
@Suppress("unused")
internal abstract class DetektConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply(libs.plugins.detekt.get().pluginId)

        extensions.configure<DetektExtension> {
            buildUponDefaultConfig.set(true)
            config.setFrom(rootProject.file(DETEKT_CONFIG_PATH))
        }
        plugins.withType(KotlinBasePlugin::class.java) {
            extensions.configure<DetektExtension> { source.setFrom(kotlinSourceDirectories()) }
        }
    }
}

private fun Project.kotlinSourceDirectories() = provider {
    val buildDirectory = layout.buildDirectory.get().asFile
    extensions.getByType<KotlinSourceSetContainer>().sourceSets.map { sourceSet ->
        sourceSet.kotlin.sourceDirectories.filter { directory ->
            !directory.startsWith(buildDirectory) && !directory.isResourcesFolderOf(projectDir)
        }
    }
}

private fun File.isResourcesFolderOf(projectDir: File): Boolean {
    val segments = relativeTo(projectDir).invariantSeparatorsPath.split('/')
    return segments.size > RESOURCES_SEGMENT_INDEX &&
        segments[SOURCE_SEGMENT_INDEX] == SOURCE_FOLDER_NAME &&
        segments[RESOURCES_SEGMENT_INDEX] == RESOURCES_FOLDER_NAME
}
