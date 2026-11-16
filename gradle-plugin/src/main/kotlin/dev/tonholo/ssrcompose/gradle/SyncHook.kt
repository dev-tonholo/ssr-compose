package dev.tonholo.ssrcompose.gradle

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.tasks.TaskProvider
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.gradle.ext.IdeaExtPlugin
import org.jetbrains.gradle.ext.ProjectSettings
import org.jetbrains.gradle.ext.TaskTriggersConfig

internal object SyncHook {
    private const val SETTINGS_EXTENSION_NAME = "settings"
    private const val TASK_TRIGGERS_EXTENSION_NAME = "taskTriggers"
    private const val SYNC_TASK_NAME = "ssrCatalogSync"
    private const val SYNC_TASK_GROUP = "ssr"
    private const val SYNC_TASK_DESCRIPTION = "Fetches and pins declared SSR Compose catalogs."
    private const val STAND_IN_LOG_MESSAGE = "ssrCatalogSync stand-in executed"
    private const val MISSING_TRIGGER_API_WARNING =
        "idea-ext was applied on the root project, but its taskTriggers API was not found; " +
            "IDE sync will not run ssrCatalogSync automatically."

    fun registerIfProducer(project: Project, extension: SsrComposeExtension) {
        if (!extension.isProducerConfigured()) return
        val syncTask = registerStandInSyncTask(project)
        wireIdeaAfterSync(project = project, syncTask = syncTask)
    }

    private fun registerStandInSyncTask(project: Project): TaskProvider<Task> =
        project.tasks.register(SYNC_TASK_NAME) { task ->
            task.group = SYNC_TASK_GROUP
            task.description = SYNC_TASK_DESCRIPTION
            task.doLast {
                task.logger.lifecycle(STAND_IN_LOG_MESSAGE)
            }
        }

    private fun wireIdeaAfterSync(project: Project, syncTask: TaskProvider<Task>) {
        val rootProject = project.rootProject
        rootProject.plugins.apply(IdeaExtPlugin::class.java)
        val taskTriggers = findTaskTriggers(rootProject)
        if (taskTriggers == null) {
            project.logger.warn(MISSING_TRIGGER_API_WARNING)
            return
        }
        taskTriggers.afterSync(syncTask)
    }

    private fun findTaskTriggers(rootProject: Project): TaskTriggersConfig? =
        rootProject.extensions.findByType(IdeaModel::class.java)
            ?.let { ideaModel -> ideaModel.project as? ExtensionAware }
            ?.extensions?.findByName(SETTINGS_EXTENSION_NAME)
            ?.let { settings -> settings as? ProjectSettings }
            ?.let { settings -> settings as? ExtensionAware }
            ?.extensions?.findByName(TASK_TRIGGERS_EXTENSION_NAME) as? TaskTriggersConfig
}
