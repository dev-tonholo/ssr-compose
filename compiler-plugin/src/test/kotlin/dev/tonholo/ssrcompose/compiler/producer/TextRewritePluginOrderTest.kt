package dev.tonholo.ssrcompose.compiler.producer

import androidx.compose.compiler.plugins.kotlin.ComposePluginRegistrar
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import dev.tonholo.ssrcompose.compiler.SsrCompilerPluginRegistrar
import dev.tonholo.ssrcompose.compiler.testing.CompileTestSupport
import dev.tonholo.ssrcompose.compiler.testing.ProducerTestOptions.producerOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TextRewritePluginOrderTest {
    @Test
    fun `given Compose first, when a BasicText call is compiled, then it fails with the order message`() {
        // Arrange
        val wrongOrder = CompileTestSupport(
            registrars = listOf(ComposePluginRegistrar(), SsrCompilerPluginRegistrar()),
        )

        // Act
        val result = wrongOrder.compile(fixtureName = FIXTURE, options = producerOptions())

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        assertTrue(actual = result.messages.contains(ORDER_MESSAGE), message = result.messages)
        assertFalse(actual = result.messages.contains(NOT_SUPPORTED_YET), message = result.messages)
    }

    private companion object {
        const val FIXTURE = "BasicTextOnlyCaller"
        const val ORDER_MESSAGE = "must run before the Compose compiler plugin"
        const val NOT_SUPPORTED_YET = "is not supported yet"
    }
}
