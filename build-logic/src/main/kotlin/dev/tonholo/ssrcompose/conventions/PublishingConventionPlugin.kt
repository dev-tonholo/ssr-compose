package dev.tonholo.ssrcompose.conventions

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.plugin.devel.plugins.JavaGradlePluginPlugin

private const val JAVA_COMPONENT_NAME = "java"
private const val MAVEN_PUBLICATION_NAME = "maven"
private const val VERIFY_PUBLICATIONS_TASK_NAME = "verifyMavenPublications"
private const val VERIFY_PUBLICATIONS_DESCRIPTION = "Verifies that the module declares at least one MavenPublication."

/**
 * Applies the Maven publishing plugin, sets the group shared by every published module, registers a Maven
 * publication for plain Kotlin JVM libraries, and wires a verification task into `check`. Kotlin Multiplatform and
 * Gradle plugin modules already receive their publications from their own plugins.
 *
 * Suppressed unused: Gradle creates this plugin from the declaration in `build-logic/build.gradle.kts`.
 */
@Suppress("unused")
internal abstract class PublishingConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply(MavenPublishPlugin::class.java)
        group = PROJECT_GROUP
        pluginManager.withPlugin(libs.plugins.kotlin.jvm.get().pluginId) {
            registerJvmPublicationIfNeeded()
        }
        registerVerifyPublicationsTask()
    }
}

private fun Project.registerJvmPublicationIfNeeded() {
    if (plugins.hasPlugin(JavaGradlePluginPlugin::class.java)) return
    extensions.configure<PublishingExtension> {
        publications.register<MavenPublication>(MAVEN_PUBLICATION_NAME) {
            from(components.getByName(JAVA_COMPONENT_NAME))
        }
    }
}

private fun Project.registerVerifyPublicationsTask() {
    val projectPath = path
    val publishing = extensions.getByType<PublishingExtension>()
    val verifyPublications = tasks.register<VerifyMavenPublicationsTask>(VERIFY_PUBLICATIONS_TASK_NAME) {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = VERIFY_PUBLICATIONS_DESCRIPTION
        modulePath.set(projectPath)
        publicationNames.set(
            provider {
                publishing
                    .publications
                    .withType<MavenPublication>()
                    .names
                    .toList()
            },
        )
    }
    plugins.withType(LifecycleBasePlugin::class.java) {
        tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME).configure {
            dependsOn(verifyPublications)
        }
    }
}
