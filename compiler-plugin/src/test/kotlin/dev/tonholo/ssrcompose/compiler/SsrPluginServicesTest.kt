package dev.tonholo.ssrcompose.compiler

import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import java.util.ServiceLoader
import kotlin.test.Test
import kotlin.test.assertTrue

class SsrPluginServicesTest {
    @Test
    fun `given the plugin on the classpath, when registrars are looked up, then ours is found`() {
        // Arrange
        val loader = ServiceLoader.load(CompilerPluginRegistrar::class.java)

        // Act
        val found = loader.any { it is SsrCompilerPluginRegistrar }

        // Assert
        assertTrue(actual = found, message = "SsrCompilerPluginRegistrar is not listed in META-INF/services")
    }

    @Test
    fun `given the plugin on the classpath, when processors are looked up, then ours is found`() {
        // Arrange
        val loader = ServiceLoader.load(CommandLineProcessor::class.java)

        // Act
        val found = loader.any { it is SsrCommandLineProcessor }

        // Assert
        assertTrue(actual = found, message = "SsrCommandLineProcessor is not listed in META-INF/services")
    }
}
