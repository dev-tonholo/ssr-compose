package dev.tonholo.ssrcompose.conventions

import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Applies detekt on top of its default rules.
 *
 * Suppressed unused: Gradle creates this plugin from the declaration in `build-logic/build.gradle.kts`.
 */
@Suppress("unused")
internal abstract class DetektConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply(libs.plugins.detekt.get().pluginId)

        extensions.configure<DetektExtension> {
            buildUponDefaultConfig.set(true)
        }
    }
}
