package dev.tonholo.ssrcompose.compiler.consumer

import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import dev.tonholo.ssrcompose.catalog.fragment.CatalogFragment
import dev.tonholo.ssrcompose.compiler.testing.CompileTestSupport
import dev.tonholo.ssrcompose.compiler.testing.SourceText
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

class ConsumerExpectActualTest {
    @TempDir
    lateinit var fragmentsDir: File

    private val support: CompileTestSupport = CompileTestSupport()

    @Test
    fun `given a marked expect whose actual is not marked, when compiled, then it fails at the actual`() {
        // Arrange
        // language=kotlin
        val expectSource = """
            package fixtures.consumer

            import androidx.compose.runtime.Composable
            import dev.tonholo.ssrcompose.core.annotation.ConsumerComposable

            @ConsumerComposable
            @Composable
            expect fun SharedBanner(title: String)
        """.trimIndent()
        // language=kotlin
        val actualSource = """
            package fixtures.consumer

            import androidx.compose.runtime.Composable

            @Composable
            actual fun SharedBanner(title: String) {
            }
        """.trimIndent()

        // Act
        val result = support.compileExpectAndActual(
            common = SourceText(fileName = "SharedBannerExpect.kt", contents = expectSource),
            platform = SourceText(fileName = "SharedBannerActual.kt", contents = actualSource),
            options = consumerOptions(),
        )

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        val line = result.errorLines().single()
        assertTrue(actual = line.contains(other = UNMARKED_ACTUAL_LOCATION), message = result.messages)
        assertTrue(actual = line.contains(other = UNMARKED_ACTUAL_MESSAGE), message = result.messages)
        assertEquals(expected = emptyList(), actual = readFragments())
    }

    private fun consumerOptions(): List<Pair<String, String>> = FragmentsFolder(directory = fragmentsDir).options()

    private fun readFragments(): List<CatalogFragment> = FragmentsFolder(directory = fragmentsDir).read()

    private companion object {
        const val UNMARKED_ACTUAL_LOCATION = "SharedBannerActual.kt:5:1"
        const val UNMARKED_ACTUAL_MESSAGE =
            "The actual composable 'SharedBanner' must be marked @ConsumerComposable because its expect " +
                "declaration is marked @ConsumerComposable."
    }
}
