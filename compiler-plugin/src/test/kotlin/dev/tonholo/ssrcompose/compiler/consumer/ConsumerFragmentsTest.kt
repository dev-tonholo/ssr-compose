package dev.tonholo.ssrcompose.compiler.consumer

import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import dev.tonholo.ssrcompose.catalog.fragment.CatalogFragment
import dev.tonholo.ssrcompose.catalog.fragment.io.CatalogFragmentReader
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.catalog.options.CompilerPluginMode
import dev.tonholo.ssrcompose.catalog.options.CompilerPluginOptions
import dev.tonholo.ssrcompose.compiler.testing.CompileTestSupport
import dev.tonholo.ssrcompose.core.data.ValueKind
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

class ConsumerFragmentsTest {
    @TempDir
    lateinit var fragmentsDir: File

    private val support = CompileTestSupport()

    @Test
    fun `given a file with a marked composable with text parameters, when compiled, then its fragment is written`() {
        // Arrange
        val expected = CatalogFragment(
            source = "fixtures/consumer/Banner.fixture.kt",
            components = listOf(
                ComponentDescriptor(
                    name = "Banner",
                    parameters = listOf(
                        Parameter.Value(name = "title", kind = ValueKind.Text, required = true),
                        Parameter.Value(name = "subtitle", kind = ValueKind.Text, required = false),
                    ),
                ),
            ),
        )

        // Act
        val result = compile(fixtureName = "Banner")

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
        assertEquals(expected = listOf(expected), actual = readFragments())
    }

    @Test
    fun `given a file with no marked composable, when compiled, then no fragment is written`() {
        // Arrange
        val fileName = "Unmarked"

        // Act
        val result = compile(fixtureName = fileName)

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
        assertEquals(expected = emptyList(), actual = readFragments())
    }

    @Test
    fun `given a fragment written before, when the file loses its marked composable, then the fragment is removed`() {
        // Arrange
        compile(fixtureName = "Banner")

        // Act
        val result = compile(fixtureName = "Unmarked", fileName = "Banner")

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
        assertEquals(expected = emptyList(), actual = readFragments())
    }

    @Test
    fun `given a marked composable with an unsupported parameter, when compiled, then it fails and names both`() {
        // Arrange
        val fixtureName = "Counter"

        // Act
        val result = compile(fixtureName = fixtureName)

        // Assert
        assertNotEquals(illegal = ExitCode.OK, actual = result.exitCode, message = result.messages)
        val named = "The parameter count of the marked composable Counter"
        assertTrue(actual = result.messages.contains(other = named), message = result.messages)
        assertEquals(expected = emptyList(), actual = readFragments())
    }

    @Test
    fun `given an unmarked file with a name that breaks the fragment rules, when compiled, then it builds`() {
        // Arrange
        val fileName = "Odd..Name"

        // Act
        val result = compile(fixtureName = "Unmarked", fileName = fileName)

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
        assertEquals(expected = emptyList(), actual = readFragments())
    }

    @Test
    fun `given a marked file with a name that breaks the fragment rules, when compiled, then it fails naming it`() {
        // Arrange
        val fileName = "Odd..Name"

        // Act
        val result = compile(fixtureName = "Banner", fileName = fileName)

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        val named = "The file fixtures/consumer/Odd..Name.fixture.kt has marked composables"
        assertTrue(actual = result.messages.contains(other = named), message = result.messages)
        assertEquals(expected = emptyList(), actual = readFragments())
    }

    private fun compile(fixtureName: String, fileName: String = fixtureName) =
        support.compile(
            fixtureName = fixtureName,
            fileName = fileName,
            options = listOf(
                CompilerPluginOptions.MODE to CompilerPluginMode.Consumer.optionValue,
                CompilerPluginOptions.FRAGMENTS_DIR to fragmentsDir.absolutePath,
            ),
        )

    private fun readFragments(): List<CatalogFragment> =
        fragmentsDir.listFiles().orEmpty().sortedBy { it.name }.map { file ->
            CatalogFragmentReader.read(bytes = file.readBytes(), subject = "fragment ${file.name}")
        }
}
