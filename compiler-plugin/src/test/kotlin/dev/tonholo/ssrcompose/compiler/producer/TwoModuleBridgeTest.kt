package dev.tonholo.ssrcompose.compiler.producer

import androidx.compose.compiler.plugins.kotlin.ComposePluginRegistrar
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import dev.tonholo.ssrcompose.catalog.options.CatalogOptionValue
import dev.tonholo.ssrcompose.compiler.testing.CompileTestSupport
import dev.tonholo.ssrcompose.compiler.testing.scale.BridgeCreation
import dev.tonholo.ssrcompose.compiler.testing.scale.ExperimentBridgeRegistrar
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TwoModuleBridgeTest {
    @TempDir
    lateinit var workDir: File

    @Test
    fun `given public bridges, when two modules name different components, then they write one class name twice`() {
        // Arrange
        val first = compileModule(component = ALPHA, moduleName = null)

        // Act
        val second = compileModule(component = BETA, moduleName = null)

        // Assert
        val firstClasses = bridgeClasses(result = first)
        val secondClasses = bridgeClasses(result = second)
        assertEquals(expected = firstClasses.keys, actual = secondClasses.keys)
        val name = firstClasses.keys.single()
        assertFalse(actual = firstClasses.getValue(name).contentEquals(secondClasses.getValue(name)))
    }

    @Test
    fun `given classes named after the module, when two modules name one component, then their classes differ`() {
        // Arrange
        val first = compileModule(component = ALPHA, moduleName = FIRST_MODULE)

        // Act
        val second = compileModule(component = ALPHA, moduleName = SECOND_MODULE)

        // Assert
        val shared = bridgeClasses(result = first).keys.intersect(other = bridgeClasses(result = second).keys)
        assertTrue(actual = shared.isEmpty())
    }

    @Test
    fun `given an internal enum in one package, when two modules each declare it, then both write one class name`() {
        // Arrange
        val support = CompileTestSupport(registrars = listOf(ComposePluginRegistrar()))

        // Act
        val first = support.compileSource(fileName = ENUM_FILE, source = enumSource(entries = "First, Second"))
        val second = support.compileSource(fileName = ENUM_FILE, source = enumSource(entries = "First, Third"))

        // Assert
        val firstEnum = bridgeClasses(result = first)
        val secondEnum = bridgeClasses(result = second)
        assertEquals(expected = setOf(ENUM_CLASS_FILE), actual = firstEnum.keys.intersect(other = secondEnum.keys))
        assertFalse(actual = firstEnum.getValue(ENUM_CLASS_FILE).contentEquals(secondEnum.getValue(ENUM_CLASS_FILE)))
    }

    @Test
    fun `given internal bridges, when another module calls one without creating it, then it cannot reach it`() {
        // Arrange
        val first = compileModule(component = ALPHA, moduleName = FIRST_MODULE)

        // Act
        val result = CompileTestSupport(registrars = listOf(ComposePluginRegistrar())).compileSource(
            fileName = CALLER_FILE,
            source = callerSource(component = ALPHA),
            classpath = listOf(first.outputDirectory),
        )

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
    }

    @Test
    fun `given internal bridges, when a module depends on two that name one component, then it creates its own`() {
        // Arrange
        val first = compileModule(component = ALPHA, moduleName = FIRST_MODULE)
        val second = compileModule(component = ALPHA, moduleName = SECOND_MODULE)

        // Act
        val third = compileModule(
            component = ALPHA,
            moduleName = THIRD_MODULE,
            classpath = listOf(first.outputDirectory, second.outputDirectory),
        )

        // Assert
        assertEquals(expected = ExitCode.OK, actual = third.exitCode, message = third.messages)
    }

    private fun compileModule(
        component: String,
        moduleName: String?,
        classpath: List<File> = emptyList(),
    ): JvmCompilationResult {
        val index = workDir.resolve("catalog.index").apply { writeText(text = INDEX) }
        val catalog = CatalogOptionValue(version = "1", indexPath = index.absolutePath, bridgePackage = BRIDGE_PACKAGE)
        val source = callerSource(component = component)
        val registrar = ExperimentBridgeRegistrar(
            catalogs = listOf(catalog),
            creation = BridgeCreation(namedIn = source, files = 1, moduleName = moduleName),
        )
        val result = CompileTestSupport(registrars = listOf(registrar, ComposePluginRegistrar()))
            .compileSource(fileName = CALLER_FILE, source = source, classpath = classpath)
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
        return result
    }

    private fun bridgeClasses(result: JvmCompilationResult): Map<String, ByteArray> =
        result.outputDirectory.resolve(BRIDGE_FOLDER).listFiles().orEmpty().associate { it.name to it.readBytes() }

    private fun enumSource(entries: String): String =
        // language=kotlin
        """
        package app.bridge

        internal enum class Tone { $entries }
        """.trimIndent()

    private fun callerSource(component: String): String =
        // language=kotlin
        """
        package module.caller

        import androidx.compose.runtime.Composable
        import app.bridge.$component

        @Composable
        fun Screen() {
            $component(title = "text")
        }
        """.trimIndent()

    private companion object {
        const val ALPHA = "Alpha"
        const val BETA = "Beta"
        const val FIRST_MODULE = "ModuleOne"
        const val SECOND_MODULE = "ModuleTwo"
        const val THIRD_MODULE = "ModuleThree"
        const val BRIDGE_PACKAGE = "app.bridge"
        const val BRIDGE_FOLDER = "app/bridge"
        const val CALLER_FILE = "Screen.kt"
        const val ENUM_FILE = "Tone.kt"
        const val ENUM_CLASS_FILE = "Tone.class"
        const val INDEX = "component Alpha\nparam title text required\ncomponent Beta\nparam title text required\n"
    }
}
