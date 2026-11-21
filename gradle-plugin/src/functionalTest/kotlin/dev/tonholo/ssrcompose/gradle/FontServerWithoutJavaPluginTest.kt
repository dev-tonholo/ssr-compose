package dev.tonholo.ssrcompose.gradle

import dev.tonholo.ssrcompose.gradle.testing.FixtureProject
import dev.tonholo.ssrcompose.gradle.testing.FixtureSources
import dev.tonholo.ssrcompose.gradle.testing.PublishedFonts
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains

/** The server's font resources are wired into the `java` plugin's main source set only. */
class FontServerWithoutJavaPluginTest {
    @TempDir
    lateinit var root: File

    @Test
    fun `given a multiplatform server with a held bundle, when built, then it warns that resources are not wired`() {
        // Arrange
        val server = FixtureProject(directory = root.resolve("server"))
            .addFile(relativePath = "build.gradle.kts", text = buildScript())

        // Act
        val result = server.run(task = "tasks")

        // Assert
        assertContains(charSequence = result.output, other = WARNING)
    }

    private fun buildScript(): String =
        // language=kotlin
        """
        plugins {
            alias(libs.plugins.kotlin.multiplatform)
            alias(libs.plugins.ssrcompose)
        }

        kotlin {
            jvm()
        }

        ssrCompose {
            ${FixtureSources.producerBlock()}
            ${PublishedFonts.serverBlock(version = "1")}
        }
        """.trimIndent()

    private companion object {
        const val WARNING = "Font resources of module ':' are wired only for JVM modules with the java plugin"
    }
}
