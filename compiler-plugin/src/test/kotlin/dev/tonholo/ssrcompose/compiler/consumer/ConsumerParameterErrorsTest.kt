package dev.tonholo.ssrcompose.compiler.consumer

import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import dev.tonholo.ssrcompose.catalog.fragment.CatalogFragment
import dev.tonholo.ssrcompose.compiler.testing.CompileTestSupport
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

class ConsumerParameterErrorsTest {
    @TempDir
    lateinit var fragmentsDir: File

    private val support: CompileTestSupport = CompileTestSupport()

    @Test
    fun `given an unsupported parameter type, when compiled, then it fails at the parameter with reason`() {
        // Arrange
        val fixtureName = "UnsupportedParameterCard"

        // Act
        val result = compile(fixtureName = fixtureName)

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        val line = result.errorLines().single()
        assertTrue(actual = line.contains(other = UNSUPPORTED_PARAMETER_LOCATION), message = result.messages)
        assertTrue(actual = line.contains(other = "PriceCard"), message = result.messages)
        assertTrue(actual = line.contains(other = "price"), message = result.messages)
        assertEquals(expected = emptyList(), actual = readFragments())
    }

    private fun compile(fixtureName: String) =
        support.compile(
            fixtureName = fixtureName,
            options = consumerOptions(),
        )

    private fun consumerOptions(): List<Pair<String, String>> = FragmentsFolder(directory = fragmentsDir).options()

    private fun readFragments(): List<CatalogFragment> = FragmentsFolder(directory = fragmentsDir).read()

    private companion object {
        const val UNSUPPORTED_PARAMETER_LOCATION = "UnsupportedParameterCard.fixture.kt:12:5"
    }
}
