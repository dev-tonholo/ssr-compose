package dev.tonholo.ssrcompose.conventions

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

private const val SCALE_TASK_NAME = "scaleTest"
private const val SCALE_TASK_DESCRIPTION = "Runs the full size scale measurements. `check` does not run them."
private const val TEST_TASK_NAME = "test"
private const val TEST_SOURCE_SET_NAME = "test"

/**
 * Keeps tests tagged `scale` out of the `test` task, and so out of `check`, and runs them in a `scaleTest` task of
 * their own that is never up to date and prints what the tests measure.
 */
internal abstract class ScaleTestConventionPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.withPlugin(libs.plugins.kotlin.jvm.get().pluginId) {
            tasks.named<Test>(TEST_TASK_NAME) { skipsScaleTests() }
            val testSourceSet = extensions.getByType<SourceSetContainer>().getByName(TEST_SOURCE_SET_NAME)
            tasks.register<Test>(SCALE_TASK_NAME) {
                description = SCALE_TASK_DESCRIPTION
                testClassesDirs = testSourceSet.output.classesDirs
                classpath = testSourceSet.runtimeClasspath
                runsOnlyScaleTests()
            }
        }
    }
}
