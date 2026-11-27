package dev.tonholo.ssrcompose.conventions

import java.io.File
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.plugin.devel.plugins.JavaGradlePluginPlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension

private const val JAVA_COMPONENT_NAME = "java"
private const val MAVEN_PUBLICATION_NAME = "maven"
private const val VERIFY_PUBLICATIONS_TASK_NAME = "verifyMavenPublications"
private const val VERIFY_PUBLICATIONS_DESCRIPTION =
    "Verifies the MavenPublications of the module and the license files of every published archive."
private const val LICENSE_FILE_NAME = "LICENSE"
private const val NOTICE_FILE_NAME = "NOTICE"
private val ARCHIVE_EXTENSIONS = setOf("jar", "aar")

/**
 * Applies the Maven publishing plugin, sets the group shared by every published module, registers a Maven
 * publication for plain Kotlin JVM libraries, enables sources jars for JVM modules, puts the repository license files
 * into every archive task, turns on Kotlin explicit API mode for every Kotlin module, and wires a verification task
 * into `check`. Kotlin Multiplatform and Gradle plugin modules already receive their publications from their own
 * plugins.
 *
 * Suppressed unused: Gradle creates this plugin from the declaration in `build-logic/build.gradle.kts`.
 */
@Suppress("unused")
internal abstract class PublishingConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply(MavenPublishPlugin::class.java)
        group = PROJECT_GROUP
        enableExplicitApiForKotlinPlugins()
        addLicenseFilesToArchives()
        pluginManager.withPlugin(libs.plugins.kotlin.jvm.get().pluginId) {
            configureJvmSourcesJar()
            registerJvmPublicationIfNeeded()
        }
        registerVerifyPublicationsTask()
    }
}

private fun Project.enableExplicitApiForKotlinPlugins() {
    listOf(libs.plugins.kotlin.jvm, libs.plugins.kotlin.multiplatform).forEach { kotlinPlugin ->
        pluginManager.withPlugin(kotlinPlugin.get().pluginId) {
            extensions.configure<KotlinBaseExtension> { explicitApi() }
        }
    }
}

private fun Project.rootLicenseFiles(): List<File> =
    listOf(rootProject.file(LICENSE_FILE_NAME), rootProject.file(NOTICE_FILE_NAME))

private fun Project.addLicenseFilesToArchives() {
    tasks.withType<AbstractArchiveTask>().configureEach {
        from(rootLicenseFiles()) {
            into(META_INF_DIRECTORY)
        }
    }
}

private fun Project.configureJvmSourcesJar() {
    extensions.configure<JavaPluginExtension> {
        withSourcesJar()
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
    val licenseFileOfRoot = rootProject.file(LICENSE_FILE_NAME)
    val noticeFileOfRoot = rootProject.file(NOTICE_FILE_NAME)
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
        val publishedArchives = provider {
            publishing.publications
                .withType<MavenPublication>()
                .flatMap { it.artifacts }
                .filter { it.extension in ARCHIVE_EXTENSIONS }
        }
        archives.from(publishedArchives.map { artifacts -> artifacts.map { it.file } })
        archives.builtBy(publishedArchives)
        licenseFile.set(licenseFileOfRoot)
        noticeFile.set(noticeFileOfRoot)
    }
    plugins.withType(LifecycleBasePlugin::class.java) {
        tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME).configure {
            dependsOn(verifyPublications)
        }
    }
}
