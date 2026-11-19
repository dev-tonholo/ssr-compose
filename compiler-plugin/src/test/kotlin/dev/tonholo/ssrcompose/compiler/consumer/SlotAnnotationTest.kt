package dev.tonholo.ssrcompose.compiler.consumer

import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import dev.tonholo.ssrcompose.catalog.fragment.CatalogFragment
import dev.tonholo.ssrcompose.catalog.fragment.io.CatalogFragmentReader
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.catalog.model.SlotAccepts
import dev.tonholo.ssrcompose.catalog.options.CompilerPluginMode
import dev.tonholo.ssrcompose.catalog.options.CompilerPluginOptions
import dev.tonholo.ssrcompose.compiler.testing.CompileTestSupport
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

private const val CONFLICTING_SLOT_LOCATION = "SlotBothAnnotations.fixture.kt:10:5"
private const val NULLABLE_SLOT_LOCATION = "SlotNullable.fixture.kt:9:5"

class SlotAnnotationTest {
    @TempDir
    lateinit var fragmentsDir: File

    private val support = CompileTestSupport()

    @Test
    fun `given a marked composable with annotated and plain slots, when compiled, then accepts is recorded`() {
        // Arrange
        val expected = CatalogFragment(
            source = "fixtures/consumer/SlotAnnotations.fixture.kt",
            components = listOf(
                ComponentDescriptor(
                    name = "SlotContainer",
                    parameters = listOf(
                        Parameter.Slot(
                            name = "content",
                            accepts = SlotAccepts.Consumer,
                            required = true,
                        ),
                        Parameter.Slot(
                            name = "producerContent",
                            accepts = SlotAccepts.Producer,
                            required = true,
                        ),
                        Parameter.Slot(
                            name = "anyContent",
                            accepts = SlotAccepts.Any,
                            required = false,
                        ),
                    ),
                ),
            ),
        )

        // Act
        val result = compile(fixtureName = "SlotAnnotations")

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
        assertEquals(expected = listOf(expected), actual = readFragments())
    }

    @Test
    fun `given a marked composable with typealias slots, when compiled, then the fragment records each slot accepts`() {
        // Arrange
        val expected = CatalogFragment(
            source = "fixtures/consumer/SlotTypealias.fixture.kt",
            components = listOf(
                ComponentDescriptor(
                    name = "TypealiasSlotContainer",
                    parameters = listOf(
                        Parameter.Slot(
                            name = "rhsAnnotated",
                            accepts = SlotAccepts.Consumer,
                            required = true,
                        ),
                        Parameter.Slot(
                            name = "declarationAnnotated",
                            accepts = SlotAccepts.Producer,
                            required = true,
                        ),
                        Parameter.Slot(
                            name = "useSiteAnnotated",
                            accepts = SlotAccepts.Consumer,
                            required = true,
                        ),
                        Parameter.Slot(
                            name = "unannotated",
                            accepts = SlotAccepts.Any,
                            required = true,
                        ),
                    ),
                ),
            ),
        )

        // Act
        val result = compile(fixtureName = "SlotTypealias")

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
        assertEquals(expected = listOf(expected), actual = readFragments())
    }

    @Test
    fun `given a marked composable with a conflicting slot, when compiled, then it fails naming both`() {
        // Arrange
        val fixtureName = "SlotBothAnnotations"

        // Act
        val result = compile(fixtureName = fixtureName)

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        val expectedMessage = "The slot content of the marked composable ConflictingSlotContainer " +
            "has both @ConsumerComposable and @ProducerComposable"
        assertTrue(actual = result.messages.contains(other = CONFLICTING_SLOT_LOCATION), message = result.messages)
        assertTrue(actual = result.messages.contains(other = expectedMessage), message = result.messages)
        val ruleMessage = "accepts consumer composables, producer composables or either, not both"
        assertTrue(actual = result.messages.contains(other = ruleMessage), message = result.messages)
        assertEquals(expected = emptyList(), actual = readFragments())
    }

    @Test
    fun `given a marked composable with a nullable slot, when compiled, then it fails as unsupported`() {
        // Arrange
        val fixtureName = "SlotNullable"

        // Act
        val result = compile(fixtureName = fixtureName)

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        val expectedMessage = "The parameter content of the marked composable NullableSlotContainer " +
            "is not text or a composable slot."
        assertTrue(actual = result.messages.contains(other = NULLABLE_SLOT_LOCATION), message = result.messages)
        assertTrue(actual = result.messages.contains(other = expectedMessage), message = result.messages)
        assertEquals(expected = emptyList(), actual = readFragments())
    }

    @Test
    fun `given a marked composable with nested typealias slots, when compiled, then the chain is followed`() {
        // Arrange
        val expected = CatalogFragment(
            source = "fixtures/consumer/SlotNestedTypealias.fixture.kt",
            components = listOf(
                ComponentDescriptor(
                    name = "NestedTypealiasSlotContainer",
                    parameters = listOf(
                        Parameter.Slot(
                            name = "content",
                            accepts = SlotAccepts.Producer,
                            required = true,
                        ),
                    ),
                ),
            ),
        )

        // Act
        val result = compile(fixtureName = "SlotNestedTypealias")

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
        assertEquals(expected = listOf(expected), actual = readFragments())
    }

    @Test
    fun `given a marked composable with a nested typealias conflict, when compiled, then it fails naming both`() {
        // Arrange
        val fixtureName = "SlotNestedConflict"

        // Act
        val result = compile(fixtureName = fixtureName)

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        val expectedMessage = "The slot content of the marked composable NestedConflictingSlotContainer " +
            "has both @ConsumerComposable and @ProducerComposable"
        assertTrue(actual = result.messages.contains(other = expectedMessage), message = result.messages)
        val ruleMessage = "accepts consumer composables, producer composables or either, not both"
        assertTrue(actual = result.messages.contains(other = ruleMessage), message = result.messages)
        assertEquals(expected = emptyList(), actual = readFragments())
    }

    private fun compile(fixtureName: String) =
        support.compile(
            fixtureName = fixtureName,
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
