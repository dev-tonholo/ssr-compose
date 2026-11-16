package dev.tonholo.ssrcompose.conventions

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * Verifies that a module applying [PublishingConventionPlugin] declares at least one `MavenPublication`.
 */
internal abstract class VerifyMavenPublicationsTask : DefaultTask() {
    /**
     * Names of `MavenPublication` instances registered on the module.
     */
    @get:Input
    abstract val publicationNames: ListProperty<String>

    /**
     * Gradle project path of the module under verification.
     */
    @get:Input
    abstract val modulePath: Property<String>

    /**
     * Fails the build if no `MavenPublication` is registered on the module.
     */
    @TaskAction
    fun verify() {
        if (publicationNames.get().isEmpty()) {
            throw GradleException(
                "Module ${modulePath.get()} applies PublishingConventionPlugin but declares no MavenPublication.",
            )
        }
    }
}
