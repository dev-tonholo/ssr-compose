package dev.tonholo.ssrcompose.gradle

import dev.tonholo.ssrcompose.catalog.options.CompilerPluginBuildConfig
import dev.tonholo.ssrcompose.catalog.options.CompilerPluginMode
import dev.tonholo.ssrcompose.catalog.options.CompilerPluginOptions
import dev.tonholo.ssrcompose.gradle.testing.FixtureProject
import dev.tonholo.ssrcompose.gradle.testing.FixtureSources
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SsrComposePluginTest {
    @TempDir
    lateinit var projectDir: File

    private val coordinates = PluginCoordinates.load()

    @Test
    fun `given only this plugin and no dependency, when a marked function is compiled, then it builds`() {
        // Arrange
        val fixture = FixtureProject(projectDir)
            .configure(ssrComposeBlock = FixtureSources.CONSUMER_BLOCK)
            .addFile(relativePath = FixtureSources.COMPOSABLE_STAND_IN_PATH, text = FixtureSources.COMPOSABLE_STAND_IN)
            .addFile(relativePath = FixtureSources.MARKED_SOURCE_PATH, text = FixtureSources.MARKED_COMPOSABLE_SOURCE)

        // Act
        val result = fixture.compile()

        // Assert
        val outcome = result.task(":compileKotlin")?.outcome
        assertEquals(expected = TaskOutcome.SUCCESS, actual = outcome, message = result.output)
    }

    @Test
    fun `given a consumer block, when the compiler options are read, then they carry the mode and fragments folder`() {
        // Arrange
        val fixture = FixtureProject(projectDir).configure(ssrComposeBlock = FixtureSources.CONSUMER_BLOCK)

        // Act
        val setup = fixture.readCompilerSetup()

        // Assert
        val fragmentsDir = setup.optionValue(CompilerPluginOptions.FRAGMENTS_DIR).orEmpty()
        val mode = setup.optionValue(CompilerPluginOptions.MODE)
        assertEquals(expected = CompilerPluginMode.Consumer.optionValue, actual = mode)
        assertTrue(actual = fragmentsDir.endsWithPath(FRAGMENTS_FOLDER), message = fragmentsDir)
        assertEquals(expected = emptyList(), actual = setup.optionValues(CompilerPluginOptions.CATALOG))
    }

    @Test
    fun `given two catalog blocks, when the compiler options are read, then each catalog is an option in order`() {
        // Arrange
        val fixture = FixtureProject(projectDir).configure(ssrComposeBlock = FixtureSources.PRODUCER_BLOCK)

        // Act
        val setup = fixture.readCompilerSetup()

        // Assert
        val catalogs = setup.catalogOptions()
        val mode = setup.optionValue(CompilerPluginOptions.MODE)
        assertEquals(expected = CompilerPluginMode.Producer.optionValue, actual = mode)
        assertEquals(expected = listOf("2", "3"), actual = catalogs.map { it.version })
        assertEquals(expected = listOf("app.bridge.v2", "app.bridge.v3"), actual = catalogs.map { it.bridgePackage })
        val expectedIndexes = listOf("2", "3").map(::expectedIndexFile)
        assertEquals(expected = expectedIndexes, actual = catalogs.map { File(it.indexPath).canonicalFile })
    }

    @Test
    fun `given the plugin, when the compiler arguments are read, then it runs before the Compose compiler`() {
        // Arrange
        val fixture = FixtureProject(projectDir).configure(ssrComposeBlock = FixtureSources.CONSUMER_BLOCK)

        // Act
        val setup = fixture.readCompilerSetup()

        // Assert
        val pluginId = CompilerPluginBuildConfig.COMPILER_PLUGIN_ID
        val expected = "-Xcompiler-plugin-order=$pluginId>$COMPOSE_COMPILER_PLUGIN_ID"
        assertTrue(actual = expected in setup.freeCompilerArguments, message = setup.toString())
    }

    @Test
    fun `given the plugin, when the compiler plugin classpath is read, then it holds the compiler plugin`() {
        // Arrange
        val fixture = FixtureProject(projectDir).configure(ssrComposeBlock = FixtureSources.CONSUMER_BLOCK)

        // Act
        val setup = fixture.readCompilerSetup()

        // Assert
        val expected = "${coordinates.group}:${coordinates.compilerPluginArtifact}:${coordinates.version}"
        assertTrue(actual = expected in setup.pluginDependencies, message = setup.toString())
    }

    @Test
    fun `given the plugin, when the implementation dependencies are read, then core has the plugin version`() {
        // Arrange
        val fixture = FixtureProject(projectDir).configure(ssrComposeBlock = FixtureSources.CONSUMER_BLOCK)

        // Act
        val setup = fixture.readCompilerSetup()

        // Assert
        val expected = "${coordinates.group}:${coordinates.coreArtifact}:${coordinates.version}"
        assertTrue(actual = expected in setup.implementationDependencies, message = setup.toString())
    }

    @Test
    fun `given no block, when the setup is read, then the build fails and names both blocks`() {
        // Arrange
        val fixture = FixtureProject(projectDir).configure(ssrComposeBlock = "")

        // Act
        val result = fixture.runAndFail(task = "printCompilerSetup")

        // Assert
        assertTrue(actual = result.output.contains("consumer { }"), message = result.output)
        assertTrue(actual = result.output.contains("producer { }"), message = result.output)
    }

    private fun String.endsWithPath(suffix: String): Boolean = File(this).invariantSeparatorsPath.endsWith(suffix)

    private fun expectedIndexFile(version: String): File =
        File(projectDir, FixtureSources.indexPath(version)).canonicalFile

    private companion object {
        const val FRAGMENTS_FOLDER = "ssr/fragments/main"
        const val COMPOSE_COMPILER_PLUGIN_ID = "androidx.compose.compiler.plugins.kotlin"
    }
}
