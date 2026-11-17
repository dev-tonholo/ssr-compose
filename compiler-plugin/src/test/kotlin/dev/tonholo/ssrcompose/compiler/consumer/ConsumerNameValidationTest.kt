package dev.tonholo.ssrcompose.compiler.consumer

import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import dev.tonholo.ssrcompose.catalog.fragment.CatalogFragment
import dev.tonholo.ssrcompose.catalog.fragment.io.CatalogFragmentReader
import dev.tonholo.ssrcompose.catalog.options.CompilerPluginMode
import dev.tonholo.ssrcompose.catalog.options.CompilerPluginOptions
import dev.tonholo.ssrcompose.compiler.testing.CompileTestSupport
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

private const val MAX_NAME_LENGTH = 64

class ConsumerNameValidationTest {
    @TempDir
    lateinit var fragmentsDir: File

    private val support = CompileTestSupport()

    @Test
    fun `given a marked composable with a backtick slot name, when compiled, then it fails as a plugin error`() {
        // Arrange
        val fixtureName = "SlotUnsupportedName"

        // Act
        val result = compile(fixtureName = fixtureName)

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        val expectedMessage = parameterNameError(
            composable = "BacktickSlotContainer",
            parameter = "main-content",
        )
        assertTrue(actual = result.messages.contains(other = expectedMessage), message = result.messages)
        assertEquals(expected = emptyList(), actual = readFragments())
    }

    @Test
    fun `given a marked composable with a backtick text parameter, when compiled, then it fails as a plugin error`() {
        // Arrange
        val fixtureName = "ParameterUnsupportedName"

        // Act
        val result = compile(fixtureName = fixtureName)

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        val expectedMessage = parameterNameError(
            composable = "BacktickParameterBanner",
            parameter = "main-title",
        )
        assertTrue(actual = result.messages.contains(other = expectedMessage), message = result.messages)
        assertEquals(expected = emptyList(), actual = readFragments())
    }

    @Test
    fun `given a marked composable with a backtick function name, when compiled, then it fails as a plugin error`() {
        // Arrange
        val fixtureName = "ComposableUnsupportedName"

        // Act
        val result = compile(fixtureName = fixtureName)

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        val expectedMessage = composableNameError(composable = "Main-Banner")
        assertTrue(actual = result.messages.contains(other = expectedMessage), message = result.messages)
        assertEquals(expected = emptyList(), actual = readFragments())
    }

    @Test
    fun `given a composable name over the limit, when compiled, then it fails as a plugin error`() {
        // Arrange
        val longName = "a".repeat(n = MAX_NAME_LENGTH + 1)
        val source = markedComposableSource(name = longName)

        // Act
        val result = support.compileSource(
            fileName = "LongName.fixture.kt",
            source = source,
            options = consumerOptions(),
        )

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        val expectedMessage = composableNameError(composable = longName)
        assertTrue(actual = result.messages.contains(other = expectedMessage), message = result.messages)
        assertEquals(expected = emptyList(), actual = readFragments())
    }

    private fun compile(fixtureName: String) =
        support.compile(
            fixtureName = fixtureName,
            options = consumerOptions(),
        )

    private fun consumerOptions(): List<Pair<String, String>> = listOf(
        CompilerPluginOptions.MODE to CompilerPluginMode.Consumer.optionValue,
        CompilerPluginOptions.FRAGMENTS_DIR to fragmentsDir.absolutePath,
    )

    private fun readFragments(): List<CatalogFragment> =
        fragmentsDir.listFiles().orEmpty().sortedBy { it.name }.map { file ->
            CatalogFragmentReader.read(bytes = file.readBytes(), subject = "fragment ${file.name}")
        }

    private fun composableNameError(composable: String): String =
        "The marked composable $composable has a name the catalog cannot hold. " +
            "A name must be a plain Kotlin identifier of at most $MAX_NAME_LENGTH characters"

    private fun parameterNameError(composable: String, parameter: String): String =
        "The parameter $parameter of the marked composable $composable has a name the catalog cannot hold. " +
            "A name must be a plain Kotlin identifier of at most $MAX_NAME_LENGTH characters"

    private fun markedComposableSource(name: String): String {
        // language=kotlin
        return """
            package fixtures.consumer

            import androidx.compose.runtime.Composable
            import dev.tonholo.ssrcompose.core.annotation.ConsumerComposable

            @ConsumerComposable
            @Composable
            fun $name(title: String) {
            }
        """.trimIndent()
    }
}
