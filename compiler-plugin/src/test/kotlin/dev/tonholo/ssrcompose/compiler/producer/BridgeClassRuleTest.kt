package dev.tonholo.ssrcompose.compiler.producer

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.compiler.testing.CompileTestSupport
import dev.tonholo.ssrcompose.compiler.testing.ProducerTestOptions.catalog
import dev.tonholo.ssrcompose.compiler.testing.ProducerTestOptions.producerOptions
import dev.tonholo.ssrcompose.compiler.testing.TestCatalogs
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BridgeClassRuleTest {
    @Test
    fun `given a name whose hash reads as a positive number, when compiled, then it lands in its bucket`() {
        // Arrange
        val names = listOf("Greeting")

        // Act
        val classes = bridgeClassesOf(names = names)

        // Assert
        assertEquals(expected = mapOf("Greeting" to 93), actual = classes)
    }

    @Test
    fun `given a name whose hash reads as a negative number, when compiled, then its bucket is not negative`() {
        // Arrange
        val names = listOf("Card")

        // Act
        val classes = bridgeClassesOf(names = names)

        // Assert
        assertEquals(expected = mapOf("Card" to 227), actual = classes)
    }

    @Test
    fun `given a name that is not ASCII, when compiled, then its bucket comes from its UTF-8 bytes`() {
        // Arrange
        val names = listOf("Título")

        // Act
        val classes = bridgeClassesOf(names = names)

        // Assert
        assertEquals(expected = mapOf("Título" to 213), actual = classes)
    }

    @Test
    fun `given a component added to a catalog, when compiled, then no other component moves`() {
        // Arrange
        val before = bridgeClassFilesOf(names = listOf("Greeting", "Card"))

        // Act
        val after = bridgeClassFilesOf(names = listOf("Greeting", "Card", "Título"))

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
        val written = result.outputDirectory.resolve(BRIDGE_FOLDER).listFiles().orEmpty().map { bucketOf(it.name) }
        assertEquals(expected = setOf(93, 227), actual = written.toSet())
        assertEquals(expected = 2, actual = written.size)
    }

    @TestFactory
    fun `given a module name, when compiled, then the bridge class file name is safe and within the limit`() =
        MODULE_NAMES.map { moduleName ->
            dynamicTest(moduleName.take(n = SHOWN_NAME_LENGTH)) {
                // Arrange
                val names = listOf("Greeting")

                // Act
                val result = compile(names = names, moduleName = moduleName)

                // Assert
                val fileName = result.outputDirectory.resolve(BRIDGE_FOLDER).listFiles().orEmpty().single().name
                val isSafe = SAFE_CLASS_FILE_NAME.matches(input = fileName)
                val bytes = fileName.encodeToByteArray().size
                assertTrue(actual = isSafe, message = "Not a safe class file name: $fileName")
                assertTrue(actual = bytes <= MAX_FILE_NAME_BYTES, message = "Too long: $fileName")
            }
        }

    @Test
    fun `given a module name far over any file name limit, when named, then the class file stays within the limit`() {
        // Arrange
        val rule = BridgeClassRule(moduleName = "é".repeat(n = FAR_TOO_LONG_MODULE_NAME))

        // Act
        val fileNames = (0..BUCKET_SEARCH_LIMIT).map { index -> rule.fileNameOf(componentName = "Component$index") }

        // Assert
        val longest = checkNotNull(fileNames.maxByOrNull { it.length })
        val classFileBytes = "${longest}Kt.class".encodeToByteArray().size
        val reachedLastBucket = fileNames.any { it.startsWith(LONGEST_BUCKET) }
        assertTrue(actual = reachedLastBucket, message = "No name reached the last bucket")
        assertTrue(actual = classFileBytes <= MAX_FILE_NAME_BYTES, message = "$classFileBytes bytes")
    }

    @Test
    fun `given module names that read the same once cleaned, when compiled, then their classes differ`() {
        // Arrange
        val names = listOf("Greeting")

        // Act
        val dashed = classFileOf(result = compile(names = names, moduleName = "my-app"))
        val underscored = classFileOf(result = compile(names = names, moduleName = "my_app"))

        // Assert
        assertTrue(actual = dashed != underscored, message = "Both modules wrote $dashed")
    }

    @Test
    fun `given one module name, when compiled twice, then the class name stays the same`() {
        // Arrange
        val names = listOf("Greeting")

        // Act
        val first = classFileOf(result = compile(names = names, moduleName = "server"))
        val second = classFileOf(result = compile(names = names, moduleName = "server"))

        // Assert
        assertEquals(expected = first, actual = second)
    }

    private fun classFileOf(result: JvmCompilationResult): String =
        result.outputDirectory.resolve(BRIDGE_FOLDER).listFiles().orEmpty().single().name

    private fun bucketOf(classFileName: String): Int =
        classFileName.removePrefix(CLASS_PREFIX).substringBefore(BUCKET_END).toInt()

    private fun bridgeClassesOf(names: List<String>): Map<String, Int> =
        bridgeClassFilesOf(names = names).mapValues { (_, className) -> bucketOf(classFileName = className) }

    private fun bridgeClassFilesOf(names: List<String>): Map<String, String> {
        val result = compile(names = names)
        val classFiles = result.outputDirectory.resolve(BRIDGE_FOLDER).listFiles().orEmpty()
        return classFiles.map { it.nameWithoutExtension }.flatMap { className ->
            result.classLoader.loadClass("$BRIDGE_PACKAGE.$className").declaredMethods
                .map { it.name }
                .filter { it in names }
                .map { name -> name to className }
        }.toMap()
    }

    private fun compile(names: List<String>, moduleName: String? = null): JvmCompilationResult {
        val components = names.map { name ->
            ComponentDescriptor(name = name, parameters = listOf(TestCatalogs.requiredTitle))
        }
        val held = TestCatalogs.catalogOf(version = VERSION, components = components.toTypedArray())
        val result = CompileTestSupport().compileSource(
            fileName = "Anchor.kt",
            source = TestCatalogs.sourceNaming(names = names),
            options = producerOptions(catalog(catalog = held, bridgePackage = BRIDGE_PACKAGE)),
            moduleName = moduleName,
        )
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
        return result
    }

    private companion object {
        const val VERSION = "1"
        const val BRIDGE_PACKAGE = "app.bridge.rule"
        const val BRIDGE_FOLDER = "app/bridge/rule"
        const val SHOWN_NAME_LENGTH = 24
        const val BUCKET_SEARCH_LIMIT = 5_000
        const val MAX_FILE_NAME_BYTES = 255
        const val LONGEST_COMPILABLE_MODULE_NAME = 200
        const val FAR_TOO_LONG_MODULE_NAME = 1_000
        const val LONGEST_BUCKET = "Bridge255"
        const val CLASS_PREFIX = "Bridge"
        const val BUCKET_END = '_'
        val SAFE_CLASS_FILE_NAME = Regex("^Bridge\\d+_[A-Za-z0-9_]*_[0-9a-f]{16}Kt\\.class$")
        val MODULE_NAMES = listOf(
            "server",
            "my-app",
            "my.app",
            "my app",
            "<main>",
            "/Users/someone/project",
            "módulo",
            "server_test",
            "m".repeat(n = LONGEST_COMPILABLE_MODULE_NAME),
        )
    }
}
