package dev.tonholo.ssrcompose.buildlogic.conventions

import dev.tonholo.ssrcompose.buildlogic.PROJECT_GROUP
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin

/**
 * Applies the Maven publishing plugin and sets the group shared by every published module.
 */
internal abstract class PublishingConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply(MavenPublishPlugin::class.java)
        group = PROJECT_GROUP
    }
}
