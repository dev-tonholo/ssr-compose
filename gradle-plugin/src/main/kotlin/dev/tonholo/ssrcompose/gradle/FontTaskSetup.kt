package dev.tonholo.ssrcompose.gradle

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.language.base.plugins.LifecycleBasePlugin

private const val GROUP = "ssr"

internal const val SAVED_FONTS_FOLDER = "ssr/fonts"

/** Describes a font step. */
internal fun Task.describeFontStep(text: String) {
    group = GROUP
    description = text
}

/** Describes a font step and skips it, saying [skipReason], when it has nothing to do. */
internal fun Task.describeFontStep(text: String, hasWork: Boolean, skipReason: String) {
    describeFontStep(text = text)
    onlyIf(skipReason) { hasWork }
}

internal fun Project.addToCheck(task: TaskProvider<out Task>) {
    plugins.withType(LifecycleBasePlugin::class.java) {
        tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME).configure { check -> check.dependsOn(task) }
    }
}
