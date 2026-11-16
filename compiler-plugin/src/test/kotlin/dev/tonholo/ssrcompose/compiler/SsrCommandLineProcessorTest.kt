package dev.tonholo.ssrcompose.compiler

import dev.tonholo.ssrcompose.catalog.options.CompilerPluginOptions
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOptionProcessingException
import org.jetbrains.kotlin.config.CompilerConfiguration
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(CompilerConfiguration.Internals::class)
class SsrCommandLineProcessorTest {
    private val processor = SsrCommandLineProcessor()

    @Test
    fun `given a mode that is neither consumer nor producer, when it is processed, then it fails and names the mode`() {
        // Arrange
        val configuration = CompilerConfiguration()

        // Act
        val failure = assertFailsWith<CliOptionProcessingException> {
            configuration.process(optionName = CompilerPluginOptions.MODE, value = "sideways")
        }

        // Assert
        assertTrue(actual = failure.message.orEmpty().contains("sideways"), message = failure.message)
    }

    @Test
    fun `given a malformed catalog value, when it is processed, then it fails and names the catalog option`() {
        // Arrange
        val configuration = CompilerConfiguration()

        // Act
        val failure = assertFailsWith<CliOptionProcessingException> {
            configuration.process(optionName = CompilerPluginOptions.CATALOG, value = "3|only-two-fields")
        }

        // Assert
        val message = failure.message.orEmpty()
        assertTrue(actual = message.contains(CompilerPluginOptions.CATALOG), message = message)
    }

    private fun CompilerConfiguration.process(optionName: String, value: String) {
        val option: AbstractCliOption = processor.pluginOptions.single { it.optionName == optionName }
        processor.processOption(option = option, value = value, configuration = this)
    }
}
