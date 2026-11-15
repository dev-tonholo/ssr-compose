package dev.tonholo.ssrcompose.buildlogic.conventions

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Applies the Kotlin serialization compiler plugin.
 *
 * Suppressed unused: Gradle creates this plugin from the declaration in `build-logic/build.gradle.kts`.
 */
@Suppress("unused")
internal abstract class SerializationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply(libs.plugins.kotlin.serialization.get().pluginId)
    }
}
