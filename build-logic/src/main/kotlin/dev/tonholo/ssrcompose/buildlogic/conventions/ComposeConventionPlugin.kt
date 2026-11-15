package dev.tonholo.ssrcompose.buildlogic.conventions

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Applies the Kotlin Compose compiler plugin.
 */
internal abstract class ComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply(libs.plugins.kotlin.compose.get().pluginId)
    }
}
