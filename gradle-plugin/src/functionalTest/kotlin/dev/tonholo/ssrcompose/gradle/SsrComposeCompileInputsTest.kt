package dev.tonholo.ssrcompose.gradle

import dev.tonholo.ssrcompose.gradle.testing.FixtureProject
import dev.tonholo.ssrcompose.gradle.testing.FixtureSources
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class SsrComposeCompileInputsTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `given a consumer block, when the compile inputs are read, then no input holds a path of the project`() {
        // Arrange
        val fixture = FixtureProject(projectDir).configure(ssrComposeBlock = FixtureSources.CONSUMER_BLOCK)

        // Act
        val setup = fixture.readCompilerSetup()

        // Assert
        assertNoProjectPath(inputProperties = setup.inputProperties)
    }

    @Test
    fun `given a compiled catalog, when only its version changes, then the compile runs again`() {
        // Arrange
        val fixture = compiledFixtureWith(producerBlock = FixtureSources.producerBlock(version = "2"))
        fixture.addFile(relativePath = FixtureSources.indexPath(version = "3"), text = INDEX_CONTENT)
        fixture.configure(ssrComposeBlock = FixtureSources.producerBlock(version = "3"))

        // Act
        val result = fixture.compile()

        // Assert
        assertEquals(expected = TaskOutcome.SUCCESS, actual = result.compileOutcome(), message = result.output)
    }

    @Test
    fun `given a compiled catalog, when only its bridge package changes, then the compile runs again`() {
        // Arrange
        val oneBridge = FixtureSources.producerBlock(bridgePackage = "app.bridge.one")
        val fixture = compiledFixtureWith(producerBlock = oneBridge)
        fixture.configure(ssrComposeBlock = FixtureSources.producerBlock(bridgePackage = "app.bridge.two"))

        // Act
        val result = fixture.compile()

        // Assert
        assertEquals(expected = TaskOutcome.SUCCESS, actual = result.compileOutcome(), message = result.output)
    }

    @Test
    fun `given a compiled catalog, when nothing changes, then the compile is up to date`() {
        // Arrange
        val fixture = compiledFixtureWith(producerBlock = FixtureSources.producerBlock())

        // Act
        val result = fixture.compile()

        // Assert
        assertEquals(expected = TaskOutcome.UP_TO_DATE, actual = result.compileOutcome(), message = result.output)
    }

    @Test
    fun `given a compiled catalog, when the content of its index changes, then the compile runs again`() {
        // Arrange
        val fixture = compiledFixtureWith(producerBlock = FixtureSources.producerBlock())
        fixture.addFile(relativePath = FixtureSources.indexPath(), text = CHANGED_INDEX_CONTENT)

        // Act
        val result = fixture.compile()

        // Assert
        assertEquals(expected = TaskOutcome.SUCCESS, actual = result.compileOutcome(), message = result.output)
    }

    private fun compiledFixtureWith(producerBlock: String): FixtureProject {
        val fixture = FixtureProject(projectDir)
            .configure(ssrComposeBlock = producerBlock)
            .addFile(relativePath = FixtureSources.MARKED_SOURCE_PATH, text = FixtureSources.MARKED_SOURCE)
            .addFile(relativePath = FixtureSources.indexPath(), text = INDEX_CONTENT)
        fixture.compile()
        return fixture
    }

    private fun BuildResult.compileOutcome(): TaskOutcome? = task(":compileKotlin")?.outcome

    private fun assertNoProjectPath(inputProperties: List<String>) {
        val projectPaths = setOf(projectDir.absolutePath, projectDir.canonicalPath)
        val offenders = inputProperties.filter { property -> projectPaths.any { property.contains(it) } }
        assertEquals(expected = emptyList(), actual = offenders, message = "inputs hold a project path")
    }

    private companion object {
        const val INDEX_CONTENT = "# first content"
        const val CHANGED_INDEX_CONTENT = "# second content"
    }
}
