package dev.tonholo.ssrcompose.compiler.producer

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import dev.tonholo.ssrcompose.compiler.testing.CompileTestSupport
import dev.tonholo.ssrcompose.compiler.testing.ProducerTestOptions.catalog
import dev.tonholo.ssrcompose.compiler.testing.ProducerTestOptions.producerOptions
import dev.tonholo.ssrcompose.compiler.testing.TestCatalogs
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BridgeCompositionTest {
    private lateinit var compiled: JvmCompilationResult

    @BeforeTest
    fun compileRunner() {
        compiled = CompileTestSupport().compile(
            fixtureName = RUNNER_FIXTURE,
            options = producerOptions(
                catalog(catalog = TestCatalogs.first, bridgePackage = FIRST_PACKAGE),
                catalog(catalog = TestCatalogs.second, bridgePackage = SECOND_PACKAGE),
                catalog(catalog = TestCatalogs.defaultedTitle, bridgePackage = DEFAULTED_PACKAGE),
            ),
            friendPaths = listOf(producerClasses()),
        )
        assertEquals(expected = ExitCode.OK, actual = compiled.exitCode, message = compiled.messages)
    }

    @Test
    fun `given a created bridge with every parameter, when composed, then the node holds its version and values`() {
        // Arrange
        val expected = "[Greeting@1{title=Welcome, subtitle=Details}" +
            "[slot content[Greeting@1{title=Nested}[]]]]"

        // Act
        val actual = run(function = "everyParameter")

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given two created bridges with one component name, when composed, then each node passes its own version`() {
        // Arrange
        val expected = "[Greeting@1{title=Welcome}[], Greeting@2{title=Welcome, badge=New}[]]"

        // Act
        val actual = run(function = "twoBridges")

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a created bridge with optional parameters left out, when composed, then they are absent`() {
        // Arrange
        val expected = "[Greeting@1{title=Welcome}[]]"

        // Act
        val actual = run(function = "optionalLeftOut")

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a text the app defaults, when the bridge call leaves it out, then no value is sent`() {
        // Arrange
        val expected = "[Banner@4{}[]]"

        // Act
        val actual = run(function = "defaultedTitleLeftOut")

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a text the app defaults, when the bridge call gives it, then the node holds the given value`() {
        // Arrange
        val expected = "[Banner@4{title=Hello}[]]"

        // Act
        val actual = run(function = "defaultedTitleGiven")

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a lambda for a created RemoteAction1, when the placed action runs, then the lambda gets the argument`() {
        // Arrange
        val expected = "clicked"

        // Act
        val actual = run(function = "actionArgument")

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a created bridge reading state, when the state changes, then recomposition updates the node`() {
        // Arrange
        val expected = "[Greeting@1{title=Updated}[]]"

        // Act
        val actual = run(function = "recomposedNode")

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    private fun run(function: String): String =
        compiled.classLoader.loadClass(RUNNER_CLASS).getMethod(function).invoke(null) as String

    private fun producerClasses(): File =
        File(Class.forName(PRODUCER_CLASS).protectionDomain.codeSource.location.toURI())

    private companion object {
        const val RUNNER_FIXTURE = "BridgeCompositionRunner"
        const val RUNNER_CLASS = "app.caller.BridgeCompositionRunner"
        const val PRODUCER_CLASS = "dev.tonholo.ssrcompose.producer.ConsumerComponentKt"
        const val FIRST_PACKAGE = "app.bridge.v1"
        const val SECOND_PACKAGE = "app.bridge.v2"
        const val DEFAULTED_PACKAGE = "app.bridge.v4"
    }
}
