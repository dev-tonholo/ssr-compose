package dev.tonholo.ssrcompose.compiler.producer

import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import dev.tonholo.ssrcompose.compiler.testing.CompileTestSupport
import dev.tonholo.ssrcompose.compiler.testing.ProducerTestOptions.producerOptions
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UnsupportedTextOverloadTest {
    @Test
    fun `given a BasicText overload with no copy, when called in producer mode, then it fails naming the overload`() {
        // Arrange
        val support = CompileTestSupport()

        // Act
        val result = support.compile(fixtureName = FIXTURE, options = producerOptions())

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        assertTrue(actual = result.messages.contains(OVERLOAD_NAME), message = result.messages)
        assertTrue(actual = result.messages.contains(NOT_SUPPORTED_YET), message = result.messages)
    }

    private companion object {
        const val FIXTURE = "UnsupportedBasicTextOverload"
        const val OVERLOAD_NAME = "BasicText(label: Int)"
        const val NOT_SUPPORTED_YET = "is not supported yet"
    }
}
