package dev.tonholo.ssrcompose.gradle

import dev.tonholo.ssrcompose.gradle.tasks.FontResourcesTask
import dev.tonholo.ssrcompose.gradle.tasks.FontSyncTask
import dev.tonholo.ssrcompose.gradle.tasks.FontVerifyTask
import dev.tonholo.ssrcompose.tooling.font.FontStepNames
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin

/**
 * Registers the server's font steps. With no held font bundle, the sync and verify steps are skipped, and the
 * resources step empties its output so that bundles held earlier do not stay in the server's resources.
 */
internal object ServerFontTasks {
    private const val GROUP = "ssr"
    private const val SAVED_FOLDER = "ssr/fonts"
    private const val LOCK_FILE = "ssr/fonts.lock"
    private const val RESOURCES_FOLDER = "ssr/font-resources"
    private const val NO_BUNDLE_REASON = "no font bundle is declared in ssrCompose { fonts { } }"

    /** @return `ssrFontSync`, which runs on IDE sync next to `ssrCatalogSync`. */
    fun register(project: Project, settings: FontSettings): TaskProvider<FontSyncTask> {
        val bundles = settings.heldBundles.toMap()
        val versions = bundles.keys.toList()
        val hasBundles = bundles.isNotEmpty()
        val savedRoot = project.layout.projectDirectory.dir(SAVED_FOLDER)
        val lockFile = project.layout.projectDirectory.file(LOCK_FILE)
        val sync = project.tasks.register(FontStepNames.SYNC, FontSyncTask::class.java) { task ->
            task.describe(text = "Fetches, checks and pins the held font bundles.", hasBundles = hasBundles)
            task.bundles.set(bundles)
            task.baseFolder.set(project.layout.projectDirectory)
            task.savedRoot.set(savedRoot)
            task.lockFile.set(lockFile)
        }
        val verify = project.tasks.register(FontStepNames.VERIFY, FontVerifyTask::class.java) { task ->
            task.describe(text = "Checks the saved font bundles against ssr/fonts.lock.", hasBundles = hasBundles)
            task.versions.set(versions)
            task.savedRoot.set(savedRoot)
            task.lockFile.set(lockFile)
        }
        val resources = project.tasks.register(FontStepNames.RESOURCES, FontResourcesTask::class.java) { task ->
            task.describe(text = "Puts the saved font bundles in the server's resources.")
            task.versions.set(versions)
            task.savedBundles.from(savedRoot)
            task.savedRoot.set(savedRoot)
            task.lockFile.set(lockFile)
            task.lockFiles.from(lockFile)
            task.outputFolder.set(project.layout.buildDirectory.dir(RESOURCES_FOLDER))
        }
        partOfCheck(project = project, verify = verify)
        intoMainResources(project = project, resources = resources)
        warnWhenNotWired(project = project, hasBundles = hasBundles)
        return sync
    }

    private fun Task.describe(text: String, hasBundles: Boolean) {
        describe(text = text)
        onlyIf(NO_BUNDLE_REASON) { hasBundles }
    }

    private fun Task.describe(text: String) {
        group = GROUP
        description = text
    }

    private fun partOfCheck(project: Project, verify: TaskProvider<FontVerifyTask>) {
        project.plugins.withType(LifecycleBasePlugin::class.java) {
            project.tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME).configure { check -> check.dependsOn(verify) }
        }
    }

    private fun warnWhenNotWired(project: Project, hasBundles: Boolean) {
        if (hasBundles && !project.plugins.hasPlugin(JavaPlugin::class.java)) {
            project.logger.warn(
                "Font resources of module '${project.path}' are wired only for JVM modules with the java plugin. " +
                    "Add ${FontStepNames.RESOURCES} output to the server's resources yourself.",
            )
        }
    }

    private fun intoMainResources(project: Project, resources: TaskProvider<FontResourcesTask>) {
        project.plugins.withType(JavaPlugin::class.java) {
            project.extensions.getByType(SourceSetContainer::class.java).named(SourceSet.MAIN_SOURCE_SET_NAME) { main ->
                main.resources.srcDir(resources.flatMap { task -> task.outputFolder })
            }
        }
    }
}
