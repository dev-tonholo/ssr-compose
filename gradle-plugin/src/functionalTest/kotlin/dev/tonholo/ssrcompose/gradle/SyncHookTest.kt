package dev.tonholo.ssrcompose.gradle

import dev.tonholo.ssrcompose.gradle.testing.FixtureIndexes
import dev.tonholo.ssrcompose.gradle.testing.FixtureProject
import dev.tonholo.ssrcompose.gradle.testing.FixtureSources
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class SyncHookTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `given a producer block, when sync triggers are inspected, then afterSync exports ssrCatalogSync`() {
        // Arrange
        val fixture = FixtureProject(projectDir)
            .configure(
                ssrComposeBlock = FixtureSources.producerBlock(),
                extraBuildScript = INSPECT_SYNC_TRIGGERS_SCRIPT,
            )

        // Act
        val result = fixture.run(task = INSPECT_SYNC_TRIGGERS_TASK)

        // Assert
        val afterSyncTasks = parseAfterSyncEntries(result.output)
        assertEquals(expected = listOf(SYNC_TASK_NAME), actual = afterSyncTasks.map { it.first })
    }

    @Test
    fun `given a consumer block, when sync triggers are inspected, then afterSync is empty`() {
        // Arrange
        val fixture = FixtureProject(projectDir)
            .configure(
                ssrComposeBlock = FixtureSources.CONSUMER_BLOCK,
                extraBuildScript = INSPECT_SYNC_TRIGGERS_SCRIPT,
            )

        // Act
        val result = fixture.run(task = INSPECT_SYNC_TRIGGERS_TASK)

        // Assert
        val afterSyncTasks = parseAfterSyncEntries(result.output)
        assertEquals(expected = emptyList(), actual = afterSyncTasks)
    }

    @Test
    fun `given a producer block, when ssrCatalogSync runs, then the build succeeds`() {
        // Arrange
        val fixture = FixtureProject(projectDir)
            .configure(ssrComposeBlock = FixtureSources.producerBlock())
            .addFile(
                relativePath = FixtureSources.indexPath(),
                bytes = FixtureIndexes.withoutComponents(marker = INDEX_MARKER),
            )
            .addFile(relativePath = PRODUCER_SOURCE_PATH, text = PRODUCER_SOURCE)

        // Act
        val compileResult = fixture.compile()
        val syncResult = fixture.run(task = SYNC_TASK_NAME)

        // Assert
        assertEquals(expected = TaskOutcome.SUCCESS, actual = compileResult.task(COMPILE_TASK_PATH)?.outcome)
        assertEquals(expected = TaskOutcome.SUCCESS, actual = syncResult.task(EXPECTED_SYNC_TASK_PATH)?.outcome)
    }

    @Test
    fun `given a producer block, when sync and trigger inspection run twice with config cache, then both succeed`() {
        // Arrange
        val fixture = FixtureProject(projectDir)
            .configure(
                ssrComposeBlock = FixtureSources.producerBlock(),
                extraBuildScript = INSPECT_SYNC_TRIGGERS_SCRIPT,
            )

        // Act
        val firstRun = fixture.run(task = INSPECT_SYNC_TRIGGERS_TASK, SYNC_TASK_NAME, CONFIGURATION_CACHE_FLAG)
        val secondRun = fixture.run(task = INSPECT_SYNC_TRIGGERS_TASK, SYNC_TASK_NAME, CONFIGURATION_CACHE_FLAG)

        // Assert
        assertEquals(expected = TaskOutcome.SUCCESS, actual = firstRun.task(EXPECTED_SYNC_TASK_PATH)?.outcome)
        assertEquals(expected = TaskOutcome.SUCCESS, actual = secondRun.task(EXPECTED_SYNC_TASK_PATH)?.outcome)
        val afterSyncTasks = parseAfterSyncEntries(secondRun.output)
        assertEquals(expected = listOf(SYNC_TASK_NAME), actual = afterSyncTasks.map { it.first })
    }

    @Test
    fun `given a multi-module server, when root sync triggers are inspected, then afterSync exports server sync`() {
        // Arrange
        val fixture = FixtureProject(projectDir)
            .addFile(relativePath = "settings.gradle.kts", text = multiModuleSettingsScript())
            .addFile(relativePath = "build.gradle.kts", text = INSPECT_SYNC_TRIGGERS_SCRIPT)
            .addFile(relativePath = "server/build.gradle.kts", text = subprojectServerBuildScript())

        // Act
        val inspectResult = fixture.run(task = INSPECT_SYNC_TRIGGERS_TASK)
        val syncResult = fixture.run(task = SERVER_SYNC_TASK_PATH)

        // Assert
        val afterSyncEntries = parseAfterSyncEntries(inspectResult.output)
        assertEquals(expected = listOf(SYNC_TASK_NAME), actual = afterSyncEntries.map { it.first })
        assertEquals(
            expected = listOf(File(projectDir, "server").canonicalPath),
            actual = afterSyncEntries.map { File(it.second).canonicalPath },
        )
        assertEquals(expected = TaskOutcome.SUCCESS, actual = syncResult.task(SERVER_SYNC_TASK_PATH)?.outcome)
    }

    @Test
    fun `given idea-ext without trigger API, when the build runs, then the output holds the warning`() {
        // Arrange
        val fixture = FixtureProject(projectDir)
            .configure(
                ssrComposeBlock = FixtureSources.producerBlock(),
                extraBuildScript = DETACH_IDEA_PROJECT_SCRIPT,
            )

        // Act
        val result = fixture.run(task = SYNC_TASK_NAME)

        // Assert
        assertContains(charSequence = result.output, other = MISSING_TRIGGER_API_WARNING)
    }

    private fun parseAfterSyncEntries(output: String): List<Pair<String, String>> =
        output.lines()
            .filter { line -> line.startsWith(AFTER_SYNC_PREFIX) }
            .map { line ->
                val parts = line.removePrefix(AFTER_SYNC_PREFIX).split(ENTRY_SEPARATOR)
                parts[0] to parts[1]
            }

    private fun multiModuleSettingsScript(): String {
        val baseSettings = File(projectDir, "settings.gradle.kts").readText()
        return "$baseSettings\ninclude(\":server\")\n"
    }

    // language=kotlin
    private fun subprojectServerBuildScript(): String = """
        plugins {
            alias(libs.plugins.kotlin.jvm)
            alias(libs.plugins.ssrcompose)
        }

        ssrCompose {
            ${FixtureSources.producerBlock()}
        }
    """.trimIndent()

    private companion object {
        const val SYNC_TASK_NAME = "ssrCatalogSync"
        const val EXPECTED_SYNC_TASK_PATH = ":$SYNC_TASK_NAME"
        const val SERVER_SYNC_TASK_PATH = ":server:$SYNC_TASK_NAME"
        const val COMPILE_TASK_PATH = ":compileKotlin"
        const val INSPECT_SYNC_TRIGGERS_TASK = "inspectSyncTriggers"
        const val AFTER_SYNC_PREFIX = "AFTER_SYNC|"
        const val ENTRY_SEPARATOR = "|"
        const val IDEA_EXT_PLUGIN_ID = "org.jetbrains.gradle.plugin.idea-ext"
        const val MISSING_TRIGGER_API_WARNING = "its taskTriggers API was not found"
        const val CONFIGURATION_CACHE_FLAG = "--configuration-cache"
        const val INDEX_MARKER = "IndexContent"
        const val PRODUCER_SOURCE_PATH = "src/main/kotlin/ServerScreen.kt"
        // language=kotlin
        val PRODUCER_SOURCE = """
            fun serverGreeting(): String = "hello"
        """.trimIndent()

        // language=kotlin
        val DETACH_IDEA_PROJECT_SCRIPT = """
            plugins.withId("$IDEA_EXT_PLUGIN_ID") {
                extensions.getByType(org.gradle.plugins.ide.idea.model.IdeaModel::class.java).project = null
            }
        """.trimIndent()

        // language=kotlin
        val INSPECT_SYNC_TRIGGERS_SCRIPT = """
            abstract class InspectSyncTriggersTask : org.gradle.api.DefaultTask() {
                @get:org.gradle.api.tasks.Input
                abstract val entries: org.gradle.api.provider.ListProperty<String>

                @org.gradle.api.tasks.TaskAction
                fun run() {
                    entries.get().forEach { entry -> println("${AFTER_SYNC_PREFIX}" + entry) }
                }
            }

            tasks.register<InspectSyncTriggersTask>("$INSPECT_SYNC_TRIGGERS_TASK") {
                entries.set(
                    project.provider {
                        val idea = project.extensions.findByType(org.gradle.plugins.ide.idea.model.IdeaModel::class.java)
                        val ideaProject = idea?.project as? org.gradle.api.plugins.ExtensionAware
                        val settings = ideaProject?.extensions?.findByName("settings")
                            as? org.gradle.api.plugins.ExtensionAware
                        val triggers = settings?.extensions?.findByName("taskTriggers")
                        val triggerMap = triggers?.javaClass?.getMethod("toMap")?.invoke(triggers) as? Map<*, *>
                        (triggerMap?.get("afterSync") as? List<*>).orEmpty()
                            .mapNotNull { entry ->
                                val map = entry as? Map<*, *> ?: return@mapNotNull null
                                val taskPath = map["taskPath"]?.toString() ?: return@mapNotNull null
                                val projectPath = map["projectPath"]?.toString().orEmpty()
                                taskPath + "$ENTRY_SEPARATOR" + projectPath
                            }
                    },
                )
            }
        """.trimIndent()
    }
}
