package dev.tonholo.ssrcompose.compiler.producer

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.compiler.testing.CompileTestSupport
import dev.tonholo.ssrcompose.compiler.testing.ProducerTestOptions.catalog
import dev.tonholo.ssrcompose.compiler.testing.ProducerTestOptions.producerOptions
import dev.tonholo.ssrcompose.compiler.testing.TestCatalogs
import kotlin.test.Test
import kotlin.test.assertEquals

class BridgeClassRuleTest {
    @Test
    fun `given a name whose hash reads as a positive number, when compiled, then it lands in its bucket`() {
        // Arrange
        val names = listOf("Greeting")

        // Act
        val classes = bridgeClassesOf(names = names)

        // Assert
        assertEquals(expected = mapOf("Greeting" to "Bridge93Kt"), actual = classes)
    }

    @Test
    fun `given a name whose hash reads as a negative number, when compiled, then its bucket is not negative`() {
        // Arrange
        val names = listOf("Card")

        // Act
        val classes = bridgeClassesOf(names = names)

        // Assert
        assertEquals(expected = mapOf("Card" to "Bridge227Kt"), actual = classes)
    }

    @Test
    fun `given a name that is not ASCII, when compiled, then its bucket comes from its UTF-8 bytes`() {
        // Arrange
        val names = listOf("Título")

        // Act
        val classes = bridgeClassesOf(names = names)

        // Assert
        assertEquals(expected = mapOf("Título" to "Bridge213Kt"), actual = classes)
    }

    @Test
    fun `given a component added to a catalog, when compiled, then no other component moves`() {
        // Arrange
        val before = bridgeClassesOf(names = listOf("Greeting", "Card"))

        // Act
        val after = bridgeClassesOf(names = listOf("Greeting", "Card", "Título"))

        // Assert
        assertEquals(expected = setOf("Greeting", "Card"), actual = before.keys)
        assertEquals(expected = setOf("Greeting", "Card", "Título"), actual = after.keys)
        assertEquals(expected = before, actual = after - "Título")
    }

    @Test
    fun `given components in two buckets, when compiled, then only those two classes are written`() {
        // Arrange
        val names = listOf("Greeting", "Card")

        // Act
        val result = compile(names = names)

        // Assert
        val written = result.outputDirectory.resolve(BRIDGE_FOLDER).listFiles().orEmpty().map { it.name }.toSet()
        assertEquals(expected = setOf("Bridge93Kt.class", "Bridge227Kt.class"), actual = written)
    }

    private fun bridgeClassesOf(names: List<String>): Map<String, String> {
        val result = compile(names = names)
        val classFiles = result.outputDirectory.resolve(BRIDGE_FOLDER).listFiles().orEmpty()
        return classFiles.map { it.nameWithoutExtension }.flatMap { className ->
            result.classLoader.loadClass("$BRIDGE_PACKAGE.$className").declaredMethods
                .map { it.name }
                .filter { it in names }
                .map { name -> name to className }
        }.toMap()
    }

    private fun compile(names: List<String>): JvmCompilationResult {
        val components = names.map { name ->
            ComponentDescriptor(name = name, parameters = listOf(TestCatalogs.requiredTitle))
        }
        val held = TestCatalogs.catalogOf(version = VERSION, components = components.toTypedArray())
        val result = CompileTestSupport().compileSource(
            fileName = "Anchor.kt",
            source = ANCHOR_SOURCE,
            options = producerOptions(catalog(catalog = held, bridgePackage = BRIDGE_PACKAGE)),
        )
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
        return result
    }

    private companion object {
        const val VERSION = "1"
        const val BRIDGE_PACKAGE = "app.bridge.rule"
        const val BRIDGE_FOLDER = "app/bridge/rule"

        // language=kotlin
        const val ANCHOR_SOURCE = "package app\n\nval anchor: Int = 1\n"
    }
}
