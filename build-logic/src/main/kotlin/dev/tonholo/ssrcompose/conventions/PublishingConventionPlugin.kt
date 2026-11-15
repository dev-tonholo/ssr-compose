package dev.tonholo.ssrcompose.conventions

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin

/**
 * Applies the Maven publishing plugin and sets the group shared by every published module.
 *
 * Suppressed unused: Gradle creates this plugin from the declaration in `build-logic/build.gradle.kts`.
 */
@Suppress("unused")
internal abstract class PublishingConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply(MavenPublishPlugin::class.java)
        group = PROJECT_GROUP
    }
}
