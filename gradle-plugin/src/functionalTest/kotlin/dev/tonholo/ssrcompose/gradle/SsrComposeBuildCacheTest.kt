package dev.tonholo.ssrcompose.gradle

import dev.tonholo.ssrcompose.gradle.testing.FixtureProject
import dev.tonholo.ssrcompose.gradle.testing.FixtureSources
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class SsrComposeBuildCacheTest {
    @TempDir
    lateinit var firstFolder: File

    @TempDir
    lateinit var secondFolder: File

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

    private fun projectIn(folder: File): FixtureProject =
        FixtureProject(directory = folder, buildCacheDirectory = cacheFolder)
            .configure(ssrComposeBlock = FixtureSources.producerBlock())
            .addFile(relativePath = FixtureSources.MARKED_SOURCE_PATH, text = FixtureSources.MARKED_SOURCE)
            .addFile(relativePath = FixtureSources.indexPath(), text = INDEX_CONTENT)

    private companion object {
        const val INDEX_CONTENT = "index content"
    }
}
