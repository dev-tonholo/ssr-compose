package dev.tonholo.ssrcompose.compiler.producer

import androidx.compose.compiler.plugins.kotlin.ComposePluginRegistrar
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import dev.tonholo.ssrcompose.compiler.testing.CompileTestSupport
import dev.tonholo.ssrcompose.compiler.testing.ProducerTestOptions.catalog
import dev.tonholo.ssrcompose.compiler.testing.ProducerTestOptions.checkOptions
import dev.tonholo.ssrcompose.compiler.testing.ProducerTestOptions.producerOptions
import dev.tonholo.ssrcompose.compiler.testing.UnplaceableExpectation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class UnplaceableComponentTest {
    private val support = CompileTestSupport()
    private val options = producerOptions(
        catalog(indexName = CATALOG_INDEX, version = CATALOG_VERSION, bridgePackage = BRIDGE_PACKAGE),
    )

    @Test
    fun `given a required slot with arguments, when its bridge is called, then the error names composable and slot`() {
        // Arrange
        val fixture = UNPLACEABLE_CALLER

        // Act
        val result = support.compile(fixtureName = fixture, options = options)

        // Assert
        assertErrorNamesComposableAndSlot(result = result)
    }

    @Test
    fun `given a required slot with arguments, when its bridge is called, then the name still resolves`() {
        // Arrange
        val fixture = UNPLACEABLE_CALLER

        // Act
        val result = support.compile(fixtureName = fixture, options = options)

        // Assert
        assertNotEquals(illegal = ExitCode.OK, actual = result.exitCode, message = result.messages)
        val unresolved = result.errorLines().filter { it.contains(UNRESOLVED_REFERENCE) }
        assertEquals(expected = emptyList(), actual = unresolved, message = result.messages)
    }

    @Test
    fun `given a placeable and an unplaceable component, when a caller uses the placeable one, then it compiles`() {
        // Arrange
        val fixture = PLACEABLE_CALLER

        // Act
        val result = support.compile(fixtureName = fixture, options = options)

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
    }

    @Test
    fun `given a required slot with arguments, when a caller references its bridge, then the error names both`() {
        // Arrange
        val fixture = UNPLACEABLE_REFERENCE_CALLER

        // Act
        val result = support.compile(fixtureName = fixture, options = options)

        // Assert
        assertErrorNamesComposableAndSlot(result = result)
    }

    @Test
    fun `given a required slot with arguments, when a caller passes wrong arguments, then the error names both`() {
        // Arrange
        val fixture = UNPLACEABLE_WRONG_ARGUMENTS_CALLER

        // Act
        val result = support.compile(fixtureName = fixture, options = options)

        // Assert
        assertErrorNamesComposableAndSlot(result = result)
    }

    @Test
    fun `given a bridge from another module, when a check caller calls it, then the error names both`() {
        // Arrange
        val bridgeModule = support.compile(fixtureName = PLACEABLE_CALLER, options = options)

        // Act
        val result = support.compile(
            fixtureName = UNPLACEABLE_CALLER,
            options = checkOptions(),
            classpath = listOf(bridgeModule.outputDirectory),
        )

        // Assert
        assertEquals(expected = ExitCode.OK, actual = bridgeModule.exitCode, message = bridgeModule.messages)
        assertErrorNamesComposableAndSlot(result = result)
    }

    @Test
    fun `given a bridge from another module, when a check caller references it, then the error names both`() {
        // Arrange
        val bridgeModule = support.compile(fixtureName = PLACEABLE_CALLER, options = options)

        // Act
        val result = support.compile(
            fixtureName = UNPLACEABLE_REFERENCE_CALLER,
            options = checkOptions(),
            classpath = listOf(bridgeModule.outputDirectory),
        )

        // Assert
        assertEquals(expected = ExitCode.OK, actual = bridgeModule.exitCode, message = bridgeModule.messages)
        assertErrorNamesComposableAndSlot(result = result)
    }

    @Test
    fun `given a bridge from another module, when a check caller calls a placeable one, then it compiles`() {
        // Arrange
        val bridgeModule = support.compile(fixtureName = PLACEABLE_CALLER, options = options)

        // Act
        val result = support.compile(
            fixtureName = RECEIPT_CALLER,
            options = checkOptions(),
            classpath = listOf(bridgeModule.outputDirectory),
        )

        // Assert
        assertEquals(expected = ExitCode.OK, actual = bridgeModule.exitCode, message = bridgeModule.messages)
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
    }

    @Test
    fun `given an external bridge, when a producer with no catalog calls it, then the error names both`() {
        // Arrange
        val bridgeModule = support.compile(fixtureName = PLACEABLE_CALLER, options = options)

        // Act
        val result = support.compile(
            fixtureName = UNPLACEABLE_CALLER,
            options = producerOptions(),
            classpath = listOf(bridgeModule.outputDirectory),
        )

        // Assert
        assertEquals(expected = ExitCode.OK, actual = bridgeModule.exitCode, message = bridgeModule.messages)
        assertErrorNamesComposableAndSlot(result = result)
    }

    @Test
    fun `given an external bridge, when a producer with a catalog calls it, then its bridge resolves and it fails`() {
        // Arrange
        val bridgeModule = support.compile(fixtureName = PLACEABLE_CALLER, options = options)
        val ownOptions = producerOptions(
            catalog(indexName = OWN_CATALOG_INDEX, version = OWN_CATALOG_VERSION, bridgePackage = OWN_BRIDGE_PACKAGE),
        )

        // Act
        val result = support.compile(
            fixtureName = OWN_BRIDGE_AND_UNPLACEABLE_CALLER,
            options = ownOptions,
            classpath = listOf(bridgeModule.outputDirectory),
        )

        // Assert
        assertEquals(expected = ExitCode.OK, actual = bridgeModule.exitCode, message = bridgeModule.messages)
        assertErrorNamesComposableAndSlot(result = result)
        val unresolved = result.errorLines().filter { it.contains(UNRESOLVED_REFERENCE) }
        assertEquals(expected = emptyList(), actual = unresolved, message = result.messages)
        assertEquals(expected = 1, actual = result.errorLines().size, message = result.messages)
    }

    @Test
    fun `given a bridge from another module, when a caller running no plugin calls it, then it compiles`() {
        // Arrange
        val bridgeModule = support.compile(fixtureName = PLACEABLE_CALLER, options = options)
        val callerSupport = CompileTestSupport(registrars = listOf(ComposePluginRegistrar()))

        // Act
        val result = callerSupport.compile(
            fixtureName = UNPLACEABLE_CALLER,
            classpath = listOf(bridgeModule.outputDirectory),
        )

        // Assert
        assertEquals(expected = ExitCode.OK, actual = bridgeModule.exitCode, message = bridgeModule.messages)
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
    }

    private fun assertErrorNamesComposableAndSlot(result: JvmCompilationResult) {
        assertNotEquals(illegal = ExitCode.OK, actual = result.exitCode, message = result.messages)
        val reported = result.errorLines().any { it.endsWith(UnplaceableExpectation.MESSAGE) }
        assertTrue(actual = reported, message = result.messages)
    }

    private fun JvmCompilationResult.errorLines(): List<String> =
        messages.lines().filter { it.startsWith(ERROR_PREFIX) }

    private companion object {
        const val CATALOG_INDEX = "unplaceable-catalog.throwaway-index"
        const val CATALOG_VERSION = "3"
        const val BRIDGE_PACKAGE = "app.bridge.unplaceable"
        const val OWN_CATALOG_INDEX = "first-catalog.throwaway-index"
        const val OWN_CATALOG_VERSION = "1"
        const val OWN_BRIDGE_PACKAGE = "app.bridge.v1"
        const val UNPLACEABLE_CALLER = "UnplaceableCaller"
        const val UNPLACEABLE_REFERENCE_CALLER = "UnplaceableReferenceCaller"
        const val UNPLACEABLE_WRONG_ARGUMENTS_CALLER = "UnplaceableWrongArgumentsCaller"
        const val PLACEABLE_CALLER = "PlaceableNextToUnplaceableCaller"
        const val RECEIPT_CALLER = "ReceiptCaller"
        const val OWN_BRIDGE_AND_UNPLACEABLE_CALLER = "OwnBridgeAndUnplaceableCaller"
        const val UNRESOLVED_REFERENCE = "Unresolved reference"
        const val ERROR_PREFIX = "e: "
    }
}
