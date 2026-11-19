package dev.tonholo.ssrcompose.gradle

import dev.tonholo.ssrcompose.gradle.testing.FixtureSources
import dev.tonholo.ssrcompose.gradle.testing.NamedBridgeFixture
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NamedBridgeSourcesTest {
    @TempDir
    lateinit var projectDir: File

    @TempDir
    lateinit var cleanDir: File

    @Test
    fun `given a bridge called only from a generated folder, when another file is edited, then the bridge stays`() {
        // Arrange
        val fixture = NamedBridgeFixture(directory = projectDir, extraBuildScript = GENERATED_SOURCE_TASK)
            .writeSource(relativePath = PLAIN_SOURCE, text = PLAIN_TEXT)
        fixture.compile()
        fixture.writeSource(relativePath = PLAIN_SOURCE, text = EDITED_PLAIN_TEXT)

        // Act
        val result = fixture.compile()

        // Assert
        val outcome = result.task(COMPILE_TASK)?.outcome
        assertEquals(expected = TaskOutcome.SUCCESS, actual = outcome, message = result.output)
        assertTrue(actual = fixture.declaresBridge(component = NamedBridgeFixture.BETA))
    }

    @Test
    fun `given a file the source set excludes, when it mentions a component, then no bridge is created`() {
        // Arrange
        val fixture = NamedBridgeFixture(directory = projectDir, extraBuildScript = SOURCE_SET_EXCLUDE)
            .writeSource(relativePath = PLAIN_SOURCE, text = PLAIN_TEXT)
            .writeSource(relativePath = EXCLUDED_SOURCE, text = EXCLUDED_MENTION)

        // Act
        fixture.compile()

        // Assert
        assertFalse(actual = fixture.declaresBridge(component = NamedBridgeFixture.BETA))
    }

    @Test
    fun `given a file the compile task excludes, when it mentions a component, then no bridge is created`() {
        // Arrange
        val fixture = NamedBridgeFixture(directory = projectDir, extraBuildScript = TASK_EXCLUDE)
            .writeSource(relativePath = PLAIN_SOURCE, text = PLAIN_TEXT)
            .writeSource(relativePath = EXCLUDED_SOURCE, text = EXCLUDED_MENTION)

        // Act
        fixture.compile()

        // Assert
        assertFalse(actual = fixture.declaresBridge(component = NamedBridgeFixture.BETA))
    }

    @Test
    fun `given an excluded file, when its mention is removed, then the classes match a clean build`() {
        // Arrange
        val fixture = NamedBridgeFixture(directory = projectDir, extraBuildScript = SOURCE_SET_EXCLUDE)
            .writeSource(relativePath = PLAIN_SOURCE, text = PLAIN_TEXT)
            .writeSource(relativePath = EXCLUDED_SOURCE, text = EXCLUDED_MENTION)
        fixture.compile()
        fixture.writeSource(relativePath = EXCLUDED_SOURCE, text = EXCLUDED_WITHOUT_MENTION)
        val clean = NamedBridgeFixture(directory = cleanDir, extraBuildScript = SOURCE_SET_EXCLUDE)
            .writeSource(relativePath = PLAIN_SOURCE, text = PLAIN_TEXT)
            .writeSource(relativePath = EXCLUDED_SOURCE, text = EXCLUDED_WITHOUT_MENTION)
        clean.compile()

        // Act
        val result = fixture.compile()

        // Assert
        val outcome = result.task(COMPILE_TASK)?.outcome
        assertEquals(expected = TaskOutcome.UP_TO_DATE, actual = outcome, message = result.output)
        assertEquals(expected = clean.classFiles(), actual = fixture.classFiles())
    }

    private companion object {
        const val COMPILE_TASK = ":compileKotlin"
        const val EXCLUDED_SOURCE = "src/main/kotlin/app/Excluded.kt"

        // language=kotlin
        const val EXCLUDED_MENTION = "package app\n\n// ${NamedBridgeFixture.BETA} is only mentioned here\n"

        // language=kotlin
        const val EXCLUDED_WITHOUT_MENTION = "package app\n"

        // language=kotlin
        const val SOURCE_SET_EXCLUDE = """

            kotlin.sourceSets.named("main") { kotlin.exclude("**/Excluded.kt") }
        """

        // language=kotlin
        const val TASK_EXCLUDE = """

            tasks.named<KotlinCompile>("compileKotlin") { exclude("**/Excluded.kt") }
        """

        const val PLAIN_SOURCE = "src/main/kotlin/app/Plain.kt"

        // language=kotlin
        const val PLAIN_TEXT = "package app\n\nfun plain(): Int = 1\n"

        // language=kotlin
        const val EDITED_PLAIN_TEXT = "package app\n\nfun plain(): Int = 2\n"

        // language=kotlin
        val GENERATED_SOURCE_TASK = """

            val generateScreen = tasks.register("generateScreen") {
                val output = layout.buildDirectory.dir("generated/screens/kotlin")
                outputs.dir(output)
                doLast {
                    val file = output.get().file("app/generated/GeneratedScreen.kt").asFile
                    file.parentFile.mkdirs()
                    file.writeText(
                        ${"\"\"\""}
                        package app.generated

                        import androidx.compose.runtime.Composable
                        import ${FixtureSources.DEFAULT_BRIDGE_PACKAGE}.${NamedBridgeFixture.BETA}

                        @Composable
                        fun GeneratedScreen() {
                            ${NamedBridgeFixture.BETA}(title = "generated")
                        }
                        ${"\"\"\""}.trimIndent(),
                    )
                }
            }

            kotlin.sourceSets.named("main") { kotlin.srcDir(generateScreen) }
        """.trimIndent()
    }
}
