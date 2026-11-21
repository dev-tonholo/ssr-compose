package dev.tonholo.ssrcompose.compiler.producer

import dev.tonholo.ssrcompose.catalog.options.CompilerPluginMode
import dev.tonholo.ssrcompose.catalog.options.CompilerPluginOptions
import dev.tonholo.ssrcompose.compiler.testing.ProducerTestOptions.checkOptions
import dev.tonholo.ssrcompose.compiler.testing.text.assertWithinOnePixel
import dev.tonholo.ssrcompose.compiler.testing.text.compileTextRunner
import dev.tonholo.ssrcompose.compiler.testing.text.onAStraightScale
import dev.tonholo.ssrcompose.compiler.testing.text.onTheCurve
import dev.tonholo.ssrcompose.compiler.testing.text.probe
import kotlin.test.Test

class TextRewriteModesTest {
    @Test
    fun `given check mode, when a BasicText call is compiled, then it is rewritten like in producer mode`() {
        // Arrange
        val expected = onTheCurve()

        // Act
        val actual = compileTextRunner(options = checkOptions()).probe(screen = SCREEN)

        // Assert
        assertWithinOnePixel(expected = expected, actual = actual)
    }

    @Test
    fun `given consumer mode, when a BasicText call is compiled, then it keeps the device's drawing`() {
        // Arrange
        val expected = onAStraightScale()
        val options = listOf(CompilerPluginOptions.MODE to CompilerPluginMode.Consumer.optionValue)

        // Act
        val actual = compileTextRunner(options = options).probe(screen = SCREEN)

        // Assert
        assertWithinOnePixel(expected = expected, actual = actual)
    }

    private companion object {
        const val SCREEN = "stringPositional"
    }
}
