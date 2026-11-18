package dev.tonholo.ssrcompose.compiler.producer

import androidx.compose.compiler.plugins.kotlin.ComposePluginRegistrar
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import dev.tonholo.ssrcompose.compiler.SsrCompilerPluginRegistrar
import dev.tonholo.ssrcompose.compiler.testing.CompileTestSupport
import dev.tonholo.ssrcompose.compiler.testing.ProducerTestOptions.catalog
import dev.tonholo.ssrcompose.compiler.testing.ProducerTestOptions.producerOptions
import dev.tonholo.ssrcompose.compiler.testing.TestCatalogs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class BridgeExperimentTest {
    private val support = CompileTestSupport()
    private val firstCatalogOptions = producerOptions(
        catalog(catalog = TestCatalogs.first, bridgePackage = FIRST_PACKAGE),
    )
    private val twoCatalogOptions = producerOptions(
        catalog(catalog = TestCatalogs.first, bridgePackage = FIRST_PACKAGE),
        catalog(catalog = TestCatalogs.second, bridgePackage = SECOND_PACKAGE),
    )

    @Test
    fun `given optional parameters, when a caller gives one and leaves the other out, then it compiles`() {
        // Arrange
        val options = firstCatalogOptions

        // Act
        val result = support.compile(fixtureName = PARTIAL_OPTIONAL_CALLER, options = options)

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
    }

    @Test
    fun `given a RemoteAction1 and a slot parameter, when a caller passes a lambda for each, then it compiles`() {
        // Arrange
        val options = firstCatalogOptions

        // Act
        val result = support.compile(fixtureName = ALL_PARAMETERS_CALLER, options = options)

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
    }

    @Test
    fun `given optional parameters, when a caller leaves them out, then it compiles`() {
        // Arrange
        val options = firstCatalogOptions

        // Act
        val result = support.compile(fixtureName = REQUIRED_ONLY_CALLER, options = options)

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
    }

    @Test
    fun `given a required parameter, when a caller leaves it out, then it fails and names the parameter`() {
        // Arrange
        val options = firstCatalogOptions

        // Act
        val result = support.compile(fixtureName = MISSING_REQUIRED_CALLER, options = options)

        // Assert
        assertNotEquals(illegal = ExitCode.OK, actual = result.exitCode, message = result.messages)
        assertTrue(actual = result.messages.contains(MISSING_REQUIRED_MESSAGE), message = result.messages)
    }

    @Test
    fun `given two catalogs with the same component name, when a caller uses both, then they compile side by side`() {
        // Arrange
        val options = twoCatalogOptions

        // Act
        val result = support.compile(fixtureName = TWO_BRIDGES_CALLER, options = options)

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
    }

    @Test
    fun `given two catalogs, when a caller gives one bridge the other's parameter, then it fails`() {
        // Arrange
        val options = twoCatalogOptions

        // Act
        val result = support.compile(fixtureName = TWO_BRIDGES_WRONG_PARAMETER_CALLER, options = options)

        // Assert
        assertNotEquals(illegal = ExitCode.OK, actual = result.exitCode, message = result.messages)
        assertTrue(actual = result.messages.contains(UNKNOWN_PARAMETER_MESSAGE), message = result.messages)
    }

    @Test
    fun `given Compose registered before the bridge plugin, when compiled, then it fails with the guard message`() {
        // Arrange
        val wrongOrder = CompileTestSupport(
            registrars = listOf(ComposePluginRegistrar(), SsrCompilerPluginRegistrar()),
        )

        // Act
        val result = wrongOrder.compile(fixtureName = REQUIRED_ONLY_CALLER, options = firstCatalogOptions)

        // Assert
        assertNotEquals(illegal = ExitCode.OK, actual = result.exitCode, message = result.messages)
        assertTrue(actual = result.messages.contains(ORDER_GUARD_MESSAGE), message = result.messages)
    }

    private companion object {
        const val FIRST_PACKAGE = "app.bridge.v1"
        const val SECOND_PACKAGE = "app.bridge.v2"
        const val TWO_BRIDGES_CALLER = "TwoBridgesCaller"
        const val TWO_BRIDGES_WRONG_PARAMETER_CALLER = "TwoBridgesWrongParameterCaller"
        const val UNKNOWN_PARAMETER_MESSAGE = "No parameter with name 'badge' found"
        const val ORDER_GUARD_MESSAGE = "must run before the Compose compiler plugin"
        const val REQUIRED_ONLY_CALLER = "GreetingRequiredOnlyCaller"
        const val PARTIAL_OPTIONAL_CALLER = "GreetingPartialOptionalCaller"
        const val ALL_PARAMETERS_CALLER = "GreetingAllParametersCaller"
        const val MISSING_REQUIRED_CALLER = "GreetingMissingRequiredCaller"
        const val MISSING_REQUIRED_MESSAGE = "No value passed for parameter 'onSelect'"
    }
}
