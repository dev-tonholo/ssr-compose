package dev.tonholo.ssrcompose.gradle

import dev.tonholo.ssrcompose.gradle.testing.FixtureIndexes
import dev.tonholo.ssrcompose.gradle.testing.FixtureProject
import dev.tonholo.ssrcompose.gradle.testing.FixtureSources
import dev.tonholo.ssrcompose.gradle.testing.NamedBridgeFixture
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SsrComposeBuildCacheTest {
    @TempDir
    lateinit var firstFolder: File

    @TempDir
    lateinit var secondFolder: File

    @TempDir
    lateinit var cleanFolder: File

    @TempDir
    lateinit var cacheFolder: File

    @Test
    fun `given the same project in two folders, when the second compiles, then it takes the compile from the cache`() {
        // Arrange
        val first = projectIn(folder = firstFolder)
        val second = projectIn(folder = secondFolder)
        first.compile()

        // Act
        val result = second.compile()

        // Assert
        val outcome = result.task(":compileKotlin")?.outcome
        assertEquals(expected = TaskOutcome.FROM_CACHE, actual = outcome, message = result.output)
    }

    @Test
    fun `given named bridges in two folders, when the second compiles, then its bridge comes from the cache`() {
        // Arrange
        val first = namedBridgeProjectIn(folder = firstFolder)
        val second = namedBridgeProjectIn(folder = secondFolder)
        first.compile()

        // Act
        val result = second.compile()

        // Assert
        val outcome = result.task(":compileKotlin")?.outcome
        assertEquals(expected = TaskOutcome.FROM_CACHE, actual = outcome, message = result.output)
        assertTrue(actual = second.declaresBridge(component = NamedBridgeFixture.ALPHA))
        val projectFolders = listOf(firstFolder, secondFolder)
        assertEquals(expected = emptyList(), actual = second.classFilesHoldingPathOf(folders = projectFolders))
    }

    @Test
    fun `given a mention only in an excluded file, when another folder compiles, then the cached classes are clean`() {
        // Arrange
        val first = namedBridgeProjectIn(folder = firstFolder, extraBuildScript = EXCLUDE_FROM_COMPILE)
            .writeSource(relativePath = EXCLUDED_SOURCE, text = EXCLUDED_MENTION)
        val second = namedBridgeProjectIn(folder = secondFolder, extraBuildScript = EXCLUDE_FROM_COMPILE)
        val clean = NamedBridgeFixture(directory = cleanFolder, extraBuildScript = EXCLUDE_FROM_COMPILE).writeSource(
            relativePath = ALPHA_SCREEN,
            text = FixtureSources.bridgeScreenSource(component = NamedBridgeFixture.ALPHA, title = "cached"),
        )
        first.compile()
        clean.compile()

        // Act
        val result = second.compile()

        // Assert
        val outcome = result.task(":compileKotlin")?.outcome
        assertEquals(expected = TaskOutcome.FROM_CACHE, actual = outcome, message = result.output)
        assertEquals(expected = clean.classFiles(), actual = second.classFiles())
    }

    private fun namedBridgeProjectIn(folder: File, extraBuildScript: String = ""): NamedBridgeFixture =
        NamedBridgeFixture(
            directory = folder,
            buildCacheDirectory = cacheFolder,
            extraBuildScript = extraBuildScript,
        ).writeSource(
            relativePath = ALPHA_SCREEN,
            text = FixtureSources.bridgeScreenSource(component = NamedBridgeFixture.ALPHA, title = "cached"),
        )

    private fun projectIn(folder: File): FixtureProject =
        FixtureProject(directory = folder, buildCacheDirectory = cacheFolder)
            .configure(ssrComposeBlock = FixtureSources.producerBlock())
            .addFile(relativePath = FixtureSources.MARKED_SOURCE_PATH, text = FixtureSources.MARKED_SOURCE)
            .addFile(
                relativePath = FixtureSources.indexPath(),
                bytes = FixtureIndexes.withoutComponents(marker = INDEX_MARKER),
            )

    private companion object {
        const val INDEX_MARKER = "IndexContent"
        const val ALPHA_SCREEN = "src/main/kotlin/app/screens/AlphaScreen.kt"
        const val EXCLUDED_SOURCE = "src/main/kotlin/app/screens/Excluded.kt"

        // language=kotlin
        const val EXCLUDED_MENTION = "package app.screens\n\n// ${NamedBridgeFixture.BETA} is only mentioned here\n"

        // language=kotlin
        const val EXCLUDE_FROM_COMPILE = """

            kotlin.sourceSets.named("main") { kotlin.exclude("**/Excluded.kt") }
        """
    }
}
