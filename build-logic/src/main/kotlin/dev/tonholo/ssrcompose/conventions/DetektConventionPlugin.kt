package dev.tonholo.ssrcompose.conventions

import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer

private const val DETEKT_CONFIG_PATH = "config/detekt/detekt.yml"

/**
 * Applies detekt with the one shared config file at `config/detekt/detekt.yml` and makes the `detekt` task read every
 * Kotlin source set of the module, including Kotlin Multiplatform and extra test source sets. Generated sources under
 * the build folder are left out.
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
        sourceSet.kotlin.sourceDirectories.filter { directory -> !directory.startsWith(buildDirectory) }
    }
}
