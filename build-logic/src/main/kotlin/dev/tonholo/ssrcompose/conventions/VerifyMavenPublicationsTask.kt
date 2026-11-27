package dev.tonholo.ssrcompose.conventions

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Verifies that a module applying [PublishingConventionPlugin] declares at least one `MavenPublication`, and that
 * every published jar and AAR carries the license files of the repository.
 */
@DisableCachingByDefault(because = "Only reads its inputs and fails the build, so it has no outputs to cache")
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
     * Jars and AARs published by the module, sources jars included.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val archives: ConfigurableFileCollection

    /**
     * The repository `LICENSE`, which every published archive must carry word for word.
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val licenseFile: RegularFileProperty

    /**
     * The repository `NOTICE`, which every published archive must carry word for word.
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val noticeFile: RegularFileProperty

    /**
     * Fails the build if no `MavenPublication` is registered on the module, or if a published archive lacks a license
     * file or carries a different `LICENSE`.
     */
    @TaskAction
    fun verify() {
        if (publicationNames.get().isEmpty()) {
            throw GradleException(
                "Module ${modulePath.get()} applies PublishingConventionPlugin but declares no MavenPublication.",
            )
        }
        val problems = licenseProblemsOfArchives()
        if (problems.isNotEmpty()) {
            throw GradleException(
                "Module ${modulePath.get()} publishes archives without the license files:\n" +
                    problems.joinToString(separator = "\n"),
            )
        }
    }

    private fun licenseProblemsOfArchives(): List<String> {
        val expectedLicense = licenseFile.get().asFile.readBytes()
        val expectedNotice = noticeFile.get().asFile.readBytes()
        return archives.files.sortedBy { it.path }.flatMap { archive ->
            archiveLicenseProblems(
                archive = archive,
                expectedLicense = expectedLicense,
                expectedNotice = expectedNotice,
            ).map { "${archive.name}: $it" }
        }
    }
}
