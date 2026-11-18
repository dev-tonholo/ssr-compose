package dev.tonholo.ssrcompose.compiler.producer

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import dev.tonholo.ssrcompose.catalog.model.ChoiceList
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.catalog.options.CatalogOptionValue
import dev.tonholo.ssrcompose.catalog.testing.HandEncodedComponent
import dev.tonholo.ssrcompose.catalog.testing.HandEncodedIndex
import dev.tonholo.ssrcompose.compiler.testing.CompileTestSupport
import dev.tonholo.ssrcompose.compiler.testing.ProducerTestOptions.catalog
import dev.tonholo.ssrcompose.compiler.testing.ProducerTestOptions.producerOptions
import dev.tonholo.ssrcompose.compiler.testing.TestCatalogs
import dev.tonholo.ssrcompose.core.data.ValueKind
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BridgeCatalogRefusalTest {
    @TempDir
    lateinit var workDir: File

    @TestFactory
    fun `given a parameter the plugin cannot create, when compiled, then the error names component and parameter`() =
        REFUSED_PARAMETERS.map { parameter ->
            dynamicTest(parameter.name) {
                // Arrange
                val component = ComponentDescriptor(name = COMPONENT, parameters = listOf(parameter))
                val held = TestCatalogs.catalogOf(version = VERSION, component, choices = listOf(TONE))

                // Act
                val result = compileHolding(
                    option = catalog(catalog = held, bridgePackage = PACKAGE),
                )

                // Assert
                assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
                assertTrue(actual = result.messages.contains("'$COMPONENT'"), message = result.messages)
                assertTrue(actual = result.messages.contains("'${parameter.name}'"), message = result.messages)
            }
        }

    @Test
    fun `given a damaged index, when compiled, then the compile fails with a clear error`() {
        // Arrange
        val damaged = workDir.resolve("damaged.index").apply { writeText(text = "not an index") }
        val option = CatalogOptionValue(version = VERSION, indexPath = damaged.path, bridgePackage = PACKAGE)

        // Act
        val result = compileHolding(option = option)

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        assertTrue(actual = result.messages.contains(NOT_VALID_INDEX), message = result.messages)
    }

    @Test
    fun `given a missing index, when compiled, then the compile fails and names the catalog version`() {
        // Arrange
        val missing = workDir.resolve("missing.index")
        val option = CatalogOptionValue(version = VERSION, indexPath = missing.path, bridgePackage = PACKAGE)

        // Act
        val result = compileHolding(option = option)

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        assertTrue(actual = result.messages.contains(UNREADABLE_VERSION), message = result.messages)
    }

    @TestFactory
    fun `given a refused parameter with many choice kinds, when compiled, then the error is short and names it`() =
        BOUNDED_SHAPES.map { parameter ->
            dynamicTest(parameter.name) {
                // Arrange
                val component = ComponentDescriptor(name = COMPONENT, parameters = listOf(parameter))
                val choices = listOf(ChoiceList(name = LONG_LIST_NAME, entries = listOf("Warm")))
                val held = TestCatalogs.catalogOf(version = VERSION, component, choices = choices)

                // Act
                val result = compileHolding(option = catalog(catalog = held, bridgePackage = PACKAGE))

                // Assert
                assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
                assertTrue(actual = result.messages.contains("'$COMPONENT'"), message = result.messages)
                assertTrue(actual = result.messages.contains("'${parameter.name}'"), message = result.messages)
                assertFalse(actual = result.messages.contains(LONG_LIST_NAME), message = result.messages)
                assertTrue(actual = result.messages.length < MESSAGE_BOUND, message = "${result.messages.length}")
            }
        }

    @Test
    fun `given a component body damaged after the index opens, when compiled, then the error names the version`() {
        // Arrange
        val damaged = workDir.resolve("damaged-body.index").apply {
            writeBytes(
                array = HandEncodedIndex.encode(
                    strings = listOf(VERSION, COMPONENT, "title").map { it.encodeToByteArray() },
                    components = listOf(HandEncodedComponent(nameId = COMPONENT_NAME_ID, bodyOffset = 0)),
                    bodies = HandEncodedIndex.bodyOf(
                        HandEncodedIndex.value(nameId = PARAMETER_NAME_ID, flags = UNKNOWN_FLAG),
                    ),
                ),
            )
        }
        val option = CatalogOptionValue(version = VERSION, indexPath = damaged.path, bridgePackage = PACKAGE)

        // Act
        val result = compileHolding(option = option)

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        assertTrue(actual = result.messages.contains(UNREADABLE_VERSION), message = result.messages)
    }

    @Test
    fun `given an index of another version than its option, when compiled, then the error names both versions`() {
        // Arrange
        val held = TestCatalogs.catalogOf(version = INDEX_VERSION, SMALL_COMPONENT)
        val option = catalog(catalog = held, version = OPTION_VERSION, bridgePackage = PACKAGE)

        // Act
        val result = compileHolding(option = option)

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        assertTrue(actual = result.messages.contains("version $INDEX_VERSION"), message = result.messages)
        assertTrue(actual = result.messages.contains("version $OPTION_VERSION"), message = result.messages)
    }

    private fun compileHolding(option: CatalogOptionValue): JvmCompilationResult =
        CompileTestSupport().compileSource(
            fileName = "Anchor.kt",
            source = ANCHOR_SOURCE,
            options = producerOptions(option),
        )

    private companion object {
        const val COMPONENT = "Refused"
        const val VERSION = "1"
        const val INDEX_VERSION = "7"
        const val OPTION_VERSION = "9"
        const val PACKAGE = "app.bridge.refused"
        const val NOT_VALID_INDEX = "The catalog index is not valid"
        const val UNREADABLE_VERSION = "The index of catalog version 1 cannot be read"

        // language=kotlin
        const val ANCHOR_SOURCE = "package app\n\nval anchor: Int = 1\n"

        const val LONG_LIST_NAME = "ChoiceListWithAVeryLongNameThatAnErrorMessageMustNotRepeatEver"
        const val MANY_ARGUMENTS = 5_000
        const val MOST_CALLBACK_ARGUMENTS = 4
        const val MESSAGE_BOUND = 2_000
        const val COMPONENT_NAME_ID = 1
        const val PARAMETER_NAME_ID = 2
        const val UNKNOWN_FLAG = 4
        val LONG_CHOICE = ValueKind.Choice(listName = LONG_LIST_NAME)
        val BOUNDED_SHAPES: List<Parameter> = listOf(
            Parameter.Slot(
                name = "manyChoices",
                arguments = List(size = MANY_ARGUMENTS) { LONG_CHOICE },
                required = true,
            ),
            Parameter.Callback(
                name = "onChoice",
                arguments = List(size = MOST_CALLBACK_ARGUMENTS) { LONG_CHOICE },
                required = true,
            ),
            Parameter.Value(name = "choices", kind = ValueKind.ListOf(element = LONG_CHOICE), required = true),
        )
        val TONE = ChoiceList(name = "Tone", entries = listOf("Warm"))
        val SMALL_COMPONENT = ComponentDescriptor(name = "Small", parameters = listOf(TestCatalogs.requiredTitle))

        val REFUSED_PARAMETERS: List<Parameter> = listOf(
            Parameter.Value(name = "count", kind = ValueKind.Whole, required = true),
            Parameter.Value(name = "ratio", kind = ValueKind.Decimal, required = true),
            Parameter.Value(name = "enabled", kind = ValueKind.Flag, required = true),
            Parameter.Value(name = "tone", kind = ValueKind.Choice(listName = "Tone"), required = true),
            Parameter.Value(name = "tags", kind = ValueKind.ListOf(element = ValueKind.Text), required = true),
            Parameter.Value(name = "nullableTitle", kind = ValueKind.Text, required = true, nullable = true),
            Parameter.Callback(name = "onCancel", arguments = listOf(ValueKind.Text), required = false),
            Parameter.Callback(name = "onTap", required = true),
            Parameter.Callback(name = "onPair", arguments = listOf(ValueKind.Text, ValueKind.Text), required = true),
            Parameter.Slot(name = "optionalRow", arguments = listOf(ValueKind.Text), required = false),
            Parameter.Slot(name = "wholeRow", arguments = listOf(ValueKind.Whole), required = true),
        )
    }
}
