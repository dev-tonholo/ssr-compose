package dev.tonholo.ssrcompose.compiler.producer

import androidx.compose.compiler.plugins.kotlin.ComposePluginRegistrar
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import dev.tonholo.ssrcompose.compiler.testing.CompileTestSupport
import dev.tonholo.ssrcompose.compiler.testing.ProducerTestOptions.catalog
import dev.tonholo.ssrcompose.compiler.testing.ProducerTestOptions.producerOptions
import dev.tonholo.ssrcompose.compiler.testing.TestCatalogs
import dev.tonholo.ssrcompose.compiler.testing.UnplaceableExpectation
import java.lang.reflect.InvocationTargetException
import java.net.URLClassLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UnplaceableBridgeRunTimeTest {
    private val bridgeSupport = CompileTestSupport()
    private val callerSupport = CompileTestSupport(registrars = listOf(ComposePluginRegistrar()))
    private val bridgeOptions = producerOptions(
        catalog(catalog = TestCatalogs.unplaceable, bridgePackage = BRIDGE_PACKAGE),
    )

    @Test
    fun `given a caller built without the plugin, when it runs the bridge, then the bridge throws the message`() {
        // Arrange
        val bridgeModule = bridgeSupport.compile(fixtureName = BRIDGE_MODULE, options = bridgeOptions)
        val callerModule = callerSupport.compile(
            fixtureName = CALLER_RUNNER,
            classpath = listOf(bridgeModule.outputDirectory),
        )
        val outputs = listOf(callerModule.outputDirectory, bridgeModule.outputDirectory)
        val loader = URLClassLoader(outputs.map { it.toURI().toURL() }.toTypedArray(), javaClass.classLoader)
        val runner = loader.loadClass(RUNNER_CLASS).getMethod(RUNNER_FUNCTION)

        // Act
        val failure = assertFailsWith<InvocationTargetException> { runner.invoke(null) }

        // Assert
        assertEquals(expected = ExitCode.OK, actual = bridgeModule.exitCode, message = bridgeModule.messages)
        assertEquals(expected = ExitCode.OK, actual = callerModule.exitCode, message = callerModule.messages)
        assertEquals(expected = UnplaceableExpectation.MESSAGE, actual = failure.rootCause().message)
    }

    private fun Throwable.rootCause(): Throwable = cause?.rootCause() ?: this

    private companion object {
        const val BRIDGE_PACKAGE = "app.bridge.unplaceable"
        const val BRIDGE_MODULE = "PlaceableNextToUnplaceableCaller"
        const val CALLER_RUNNER = "UnplaceableCallerRunner"
        const val RUNNER_CLASS = "app.caller.UnplaceableCallerRunner"
        const val RUNNER_FUNCTION = "runUnplaceableCaller"
    }
}
