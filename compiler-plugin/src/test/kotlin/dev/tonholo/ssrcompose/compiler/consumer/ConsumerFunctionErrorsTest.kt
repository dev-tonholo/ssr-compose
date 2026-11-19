package dev.tonholo.ssrcompose.compiler.consumer

import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import dev.tonholo.ssrcompose.catalog.fragment.CatalogFragment
import dev.tonholo.ssrcompose.compiler.testing.CompileTestSupport
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

class ConsumerFunctionErrorsTest {
    @TempDir
    lateinit var fragmentsDir: File

    private val support: CompileTestSupport = CompileTestSupport()

    @Test
    fun `given a marked function that is not Composable, when compiled, then it fails at the function`() {
        // Arrange
        val fixtureName = "NotComposableBanner"

        // Act
        val result = compile(fixtureName = fixtureName)

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        val line = result.errorLines().single()
        assertTrue(actual = line.contains(other = NOT_COMPOSABLE_LOCATION), message = result.messages)
        assertTrue(actual = line.contains(other = NOT_COMPOSABLE_MESSAGE), message = result.messages)
        assertEquals(expected = emptyList(), actual = readFragments())
    }

    @Test
    fun `given a marked member function, when compiled, then it fails at the function`() {
        // Arrange
        val fixtureName = "MemberMarkedComposable"

        // Act
        val result = compile(fixtureName = fixtureName)

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        val line = result.errorLines().single()
        assertTrue(actual = line.contains(other = MEMBER_FUNCTION_LOCATION), message = result.messages)
        assertTrue(actual = line.contains(other = "MemberBanner"), message = result.messages)
        assertTrue(actual = line.contains(other = TOP_LEVEL_NON_GENERIC_RULE), message = result.messages)
        assertEquals(expected = emptyList(), actual = readFragments())
    }

    @Test
    fun `given a marked extension function, when compiled, then it fails at the function`() {
        // Arrange
        val fixtureName = "ExtensionMarkedComposable"

        // Act
        val result = compile(fixtureName = fixtureName)

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        val line = result.errorLines().single()
        assertTrue(actual = line.contains(other = EXTENSION_FUNCTION_LOCATION), message = result.messages)
        assertTrue(actual = line.contains(other = "Tag"), message = result.messages)
        assertTrue(actual = line.contains(other = EXTENSION_FUNCTION_RULE), message = result.messages)
        assertEquals(expected = emptyList(), actual = readFragments())
    }

    @Test
    fun `given a marked function with type parameters, when compiled, then it fails at the function`() {
        // Arrange
        val fixtureName = "GenericMarkedComposable"

        // Act
        val result = compile(fixtureName = fixtureName)

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        val line = result.errorLines().single()
        assertTrue(actual = line.contains(other = GENERIC_FUNCTION_LOCATION), message = result.messages)
        assertTrue(actual = line.contains(other = "Tag"), message = result.messages)
        assertTrue(actual = line.contains(other = TOP_LEVEL_NON_GENERIC_RULE), message = result.messages)
        assertEquals(expected = emptyList(), actual = readFragments())
    }

    @Test
    fun `given a file with a valid and a private marked composable, when compiled, then no fragment is written`() {
        // Arrange
        // language=kotlin
        val source = """
            package fixtures.consumer

            import androidx.compose.runtime.Composable
            import dev.tonholo.ssrcompose.core.annotation.ConsumerComposable

            @ConsumerComposable
            @Composable
            fun GoodCard(title: String) {
            }

            @ConsumerComposable
            @Composable
            private fun HiddenCard(title: String) {
            }
        """.trimIndent()

        // Act
        val result = support.compileSource(
            fileName = "MixedCards.fixture.kt",
            source = source,
            options = consumerOptions(),
        )

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        val line = result.errorLines().single()
        assertTrue(actual = line.contains(other = PRIVATE_FUNCTION_LOCATION), message = result.messages)
        assertTrue(actual = line.contains(other = PRIVATE_FUNCTION_MESSAGE), message = result.messages)
        assertEquals(expected = emptyList(), actual = readFragments())
    }

    @Test
    fun `given a marked local function, when compiled, then it fails at the function`() {
        // Arrange
        // language=kotlin
        val source = """
            package fixtures.consumer

            import androidx.compose.runtime.Composable
            import dev.tonholo.ssrcompose.core.annotation.ConsumerComposable

            @Composable
            fun Host() {
                @ConsumerComposable
                @Composable
                fun Local(title: String) {
                }
            }
        """.trimIndent()

        // Act
        val result = support.compileSource(
            fileName = "LocalMarkedComposable.fixture.kt",
            source = source,
            options = consumerOptions(),
        )

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        val line = result.errorLines().single()
        assertTrue(actual = line.contains(other = LOCAL_FUNCTION_LOCATION), message = result.messages)
        assertTrue(actual = line.contains(other = "'Local' is not a top-level function"), message = result.messages)
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
        const val NOT_COMPOSABLE_LOCATION = "NotComposableBanner.fixture.kt:5:1"
        const val NOT_COMPOSABLE_MESSAGE =
            "The marked function 'NotComposableBanner' is not @Composable. Only @Composable functions can be marked."
        const val MEMBER_FUNCTION_LOCATION = "MemberMarkedComposable.fixture.kt:7:5"
        const val EXTENSION_FUNCTION_LOCATION = "ExtensionMarkedComposable.fixture.kt:6:1"
        const val GENERIC_FUNCTION_LOCATION = "GenericMarkedComposable.fixture.kt:6:1"
        const val TOP_LEVEL_NON_GENERIC_RULE =
            "Only top-level composables without type parameters can be marked."
        const val LOCAL_FUNCTION_LOCATION = "LocalMarkedComposable.fixture.kt:8:5"
        const val PRIVATE_FUNCTION_LOCATION = "MixedCards.fixture.kt:11:1"
        const val PRIVATE_FUNCTION_MESSAGE =
            "The marked composable 'HiddenCard' is private. Only internal or public composables can be marked."
        const val EXTENSION_FUNCTION_RULE =
            "Only top-level composables without an extension receiver or type parameters can be marked."
    }
}
