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
        assertTrue(actual = line.contains(other = UNSUPPORTED_PARAMETER_REASON), message = result.messages)
        assertEquals(expected = emptyList(), actual = readFragments())
    }

    @Test
    fun `given a nested list parameter, when compiled, then it fails at the parameter with reason`() {
        // Arrange
        val fixtureName = "NestedListCard"

        // Act
        val result = compile(fixtureName = fixtureName)

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        val line = result.errorLines().single()
        assertTrue(actual = line.contains(other = NESTED_LIST_LOCATION), message = result.messages)
        assertTrue(actual = line.contains(other = "NestedListCard"), message = result.messages)
        assertTrue(
            actual = line.contains(other = "The parameter rows of the marked composable NestedListCard"),
            message = result.messages,
        )
        assertTrue(actual = line.contains(other = NESTED_LIST_REASON), message = result.messages)
        assertEquals(expected = emptyList(), actual = readFragments())
    }

    @Test
    fun `given a list with nullable elements, when compiled, then it fails at the parameter with reason`() {
        // Arrange
        val fixtureName = "NullableElementListCard"

        // Act
        val result = compile(fixtureName = fixtureName)

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        val line = result.errorLines().single()
        assertTrue(actual = line.contains(other = NULLABLE_ELEMENT_LIST_LOCATION), message = result.messages)
        assertTrue(actual = line.contains(other = "NullableElementListCard"), message = result.messages)
        assertTrue(
            actual = line.contains(other = "The parameter labels of the marked composable NullableElementListCard"),
            message = result.messages,
        )
        assertTrue(actual = line.contains(other = NULLABLE_ELEMENT_LIST_REASON), message = result.messages)
        assertEquals(expected = emptyList(), actual = readFragments())
    }

    @Test
    fun `given set, vararg and star projected list parameters, when compiled, then each fails at its parameter`() {
        // Arrange
        // language=kotlin
        val source = """
            package fixtures.consumer

            import androidx.compose.runtime.Composable
            import dev.tonholo.ssrcompose.core.annotation.ConsumerComposable

            @ConsumerComposable
            @Composable
            fun CollectionsCard(
                tags: Set<String>,
                vararg words: String,
                anything: List<*>,
            ) {
            }
        """.trimIndent()

        // Act
        val result = support.compileSource(
            fileName = "CollectionsCard.fixture.kt",
            source = source,
            options = consumerOptions(),
        )

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        val lines = result.errorLines()
        assertEquals(expected = COLLECTION_PARAMETER_LOCATIONS.size, actual = lines.size, message = result.messages)
        COLLECTION_PARAMETER_LOCATIONS.forEach { (location, parameter) ->
            val line = lines.single { it.contains(other = location) }
            assertTrue(
                actual = line.contains(other = "of the marked composable CollectionsCard"),
                message = result.messages,
            )
            assertTrue(actual = line.contains(other = "The parameter $parameter"), message = result.messages)
        }
        assertEquals(expected = emptyList(), actual = readFragments())
    }

    @Test
    fun `given an enum with no entries, when compiled, then it fails at the parameter naming the enum`() {
        // Arrange
        val fixtureName = "EmptyEnumCard"

        // Act
        val result = compile(fixtureName = fixtureName)

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        val line = result.errorLines().single()
        assertTrue(actual = line.contains(other = EMPTY_ENUM_LOCATION), message = result.messages)
        assertTrue(actual = line.contains(other = "EmptyEnumCard"), message = result.messages)
        assertTrue(
            actual = line.contains(other = "The parameter choice of the marked composable EmptyEnumCard"),
            message = result.messages,
        )
        assertTrue(actual = line.contains(other = EMPTY_ENUM_REASON), message = result.messages)
        assertEquals(expected = emptyList(), actual = readFragments())
    }

    @Test
    fun `given two enums with the same name and different entries in one file, when compiled, then it fails`() {
        // Arrange
        // language=kotlin
        val source = """
            package fixtures.consumer

            import androidx.compose.runtime.Composable
            import dev.tonholo.ssrcompose.core.annotation.ConsumerComposable

            object First {
                enum class Style { a, b }
            }

            object Second {
                enum class Style { a, c }
            }

            @ConsumerComposable
            @Composable
            fun ClashCard(first: First.Style, second: Second.Style) {
            }
        """.trimIndent()

        // Act
        val result = support.compileSource(
            fileName = "ClashCard.fixture.kt",
            source = source,
            options = consumerOptions(),
        )

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        val line = result.errorLines().single()
        assertTrue(actual = line.contains(other = CONFLICTING_CHOICE_LOCATION), message = result.messages)
        assertTrue(
            actual = line.contains(other = "The parameter second of the marked composable ClashCard"),
            message = result.messages,
        )
        assertTrue(actual = line.contains(other = "Style"), message = result.messages)
        assertTrue(actual = line.contains(other = CONFLICTING_CHOICE_REASON), message = result.messages)
        assertEquals(expected = emptyList(), actual = readFragments())
    }

    @Test
    fun `given an enum with a backtick entry in a marked composable, when compiled, then it fails at the parameter`() {
        // Arrange
        // language=kotlin
        val source = """
            package fixtures.consumer

            import androidx.compose.runtime.Composable
            import dev.tonholo.ssrcompose.core.annotation.ConsumerComposable

            enum class BadStyle { `body-medium` }

            @ConsumerComposable
            @Composable
            fun BadEnumCard(style: BadStyle) {
            }
        """.trimIndent()

        // Act
        val result = support.compileSource(
            fileName = "BadEnumCard.fixture.kt",
            source = source,
            options = consumerOptions(),
        )

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        val line = result.errorLines().single()
        assertTrue(actual = line.contains(other = BAD_ENUM_LOCATION), message = result.messages)
        assertTrue(
            actual = line.contains(other = "of the marked composable BadEnumCard"),
            message = result.messages,
        )
        assertTrue(actual = line.contains(other = "style"), message = result.messages)
        assertTrue(actual = line.contains(other = BAD_ENUM_REASON), message = result.messages)
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
        const val UNSUPPORTED_PARAMETER_REASON =
            "cannot be described in the catalog: its type is not a supported value, choice, list or composable slot"
        const val NESTED_LIST_LOCATION = "NestedListCard.fixture.kt:9:5"
        const val NESTED_LIST_REASON =
            "cannot be described in the catalog: a list holds text, whole numbers, decimal numbers, flags or " +
                "choices, never another list"
        const val NULLABLE_ELEMENT_LIST_LOCATION = "NullableElementListCard.fixture.kt:9:5"
        const val NULLABLE_ELEMENT_LIST_REASON =
            "cannot be described in the catalog: list elements must not be nullable"
        val COLLECTION_PARAMETER_LOCATIONS = listOf(
            "CollectionsCard.fixture.kt:9:5" to "tags",
            "CollectionsCard.fixture.kt:10:5" to "words",
            "CollectionsCard.fixture.kt:11:5" to "anything",
        )
        const val EMPTY_ENUM_LOCATION = "EmptyEnumCard.fixture.kt:11:5"
        const val EMPTY_ENUM_REASON =
            "cannot be described in the catalog: the enum EmptyChoice has no entries, and a choice list must have " +
                "at least one entry"
        const val CONFLICTING_CHOICE_LOCATION = "ClashCard.fixture.kt:16:35"
        const val CONFLICTING_CHOICE_REASON = "two different enums with that name are used in the same file"
        const val BAD_ENUM_LOCATION = "BadEnumCard.fixture.kt:10:17"
        const val BAD_ENUM_REASON =
            "cannot be described in the catalog: the enum BadStyle or one of its entries is not a plain Kotlin " +
                "identifier of at most 64 characters"
    }
}
