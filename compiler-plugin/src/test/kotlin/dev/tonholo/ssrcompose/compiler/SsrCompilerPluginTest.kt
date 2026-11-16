package dev.tonholo.ssrcompose.compiler

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import dev.tonholo.ssrcompose.catalog.options.CatalogOptionValue
import dev.tonholo.ssrcompose.catalog.options.CompilerPluginMode
import dev.tonholo.ssrcompose.catalog.options.CompilerPluginOptions
import dev.tonholo.ssrcompose.compiler.testing.CompileTestSupport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class SsrCompilerPluginTest {
    private val support = CompileTestSupport()

    @Test
    fun `given consumer options, when a fixture is compiled, then the plugin reports what it received`() {
        // Arrange
        val options = listOf(
            CompilerPluginOptions.MODE to CompilerPluginMode.Consumer.optionValue,
            CompilerPluginOptions.FRAGMENTS_DIR to "/build/ssr/fragments",
        )

        // Act
        val result = support.compile(fixtureName = GREETING_FIXTURE, options = options)

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
        assertTrue(actual = result.reports(CompilerPluginMode.Consumer), message = result.messages)
        assertTrue(actual = result.messages.contains("/build/ssr/fragments"), message = result.messages)
    }

    @Test
    fun `given producer options with two catalogs, when a fixture is compiled, then the plugin reports both`() {
        // Arrange
        val first = CatalogOptionValue(version = "2", indexPath = "/index/2", bridgePackage = "app.bridge.v2")
        val second = CatalogOptionValue(version = "3", indexPath = "/index/3", bridgePackage = "app.bridge.v3")
        val options = listOf(
            CompilerPluginOptions.MODE to CompilerPluginMode.Producer.optionValue,
            CompilerPluginOptions.CATALOG to first.encode(),
            CompilerPluginOptions.CATALOG to second.encode(),
        )

        // Act
        val result = support.compile(fixtureName = GREETING_FIXTURE, options = options)

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
        assertTrue(actual = result.reports(CompilerPluginMode.Producer), message = result.messages)
        assertTrue(actual = result.messages.contains(first.bridgePackage), message = result.messages)
        assertTrue(actual = result.messages.contains(second.bridgePackage), message = result.messages)
    }

    @Test
    fun `given no mode, when a fixture is compiled, then it fails and the message names the mode`() {
        // Arrange
        val options = emptyList<Pair<String, String>>()

        // Act
        val result = support.compile(fixtureName = GREETING_FIXTURE, options = options)

        // Assert
        assertNotEquals(illegal = ExitCode.OK, actual = result.exitCode, message = result.messages)
        assertTrue(actual = result.messages.contains(CompilerPluginOptions.MODE), message = result.messages)
    }

    private fun JvmCompilationResult.reports(mode: CompilerPluginMode): Boolean =
        messages.contains(mode.optionValue, ignoreCase = true)

    private companion object {
        const val GREETING_FIXTURE = "Greeting"
    }
}
