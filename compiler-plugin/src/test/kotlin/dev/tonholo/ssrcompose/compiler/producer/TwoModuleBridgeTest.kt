package dev.tonholo.ssrcompose.compiler.producer

import androidx.compose.compiler.plugins.kotlin.ComposePluginRegistrar
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import dev.tonholo.ssrcompose.catalog.model.Catalog
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.compiler.testing.CompileTestSupport
import dev.tonholo.ssrcompose.compiler.testing.ProducerTestOptions.catalog
import dev.tonholo.ssrcompose.compiler.testing.ProducerTestOptions.producerOptions
import dev.tonholo.ssrcompose.compiler.testing.TestCatalogs
import java.io.File
import java.net.URLClassLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TwoModuleBridgeTest {
    @Test
    fun `given two modules naming one component, when compiled, then each writes its own bridge class`() {
        // Arrange
        val first = compileModule(moduleName = FIRST_MODULE)

        // Act
        val second = compileModule(moduleName = SECOND_MODULE)

        // Assert
        val firstClasses = bridgeClasses(result = first)
        val secondClasses = bridgeClasses(result = second)
        assertEquals(expected = 1, actual = firstClasses.size)
        assertEquals(expected = 1, actual = secondClasses.size)
        assertTrue(actual = firstClasses.intersect(other = secondClasses).isEmpty())
    }

    @Test
    fun `given a bridge of another module, when a module without the plugin calls it, then it cannot reach it`() {
        // Arrange
        val first = compileModule(moduleName = FIRST_MODULE)

        // Act
        val result = CompileTestSupport(registrars = listOf(ComposePluginRegistrar())).compileSource(
            fileName = CALLER_FILE,
            source = CALLER_SOURCE,
            classpath = listOf(first.outputDirectory),
        )

        // Assert
        assertEquals(expected = ExitCode.COMPILATION_ERROR, actual = result.exitCode, message = result.messages)
        assertTrue(actual = result.messages.contains(INTERNAL_ERROR), message = result.messages)
    }

    @Test
    fun `given two modules on one classpath, when each composes its bridge, then each runs its own bridge`() {
        // Arrange
        val first = compileRunner(moduleName = FIRST_MODULE, callerPackage = FIRST_CALLER, held = CATALOG)
        val second = compileRunner(moduleName = SECOND_MODULE, callerPackage = SECOND_CALLER, held = SECOND_CATALOG)
        val outputs = listOf(first.outputDirectory, second.outputDirectory)
        val loader = URLClassLoader(outputs.map { it.toURI().toURL() }.toTypedArray(), javaClass.classLoader)

        // Act
        val firstVersion = placedVersion(loader = loader, callerPackage = FIRST_CALLER)
        val secondVersion = placedVersion(loader = loader, callerPackage = SECOND_CALLER)

        // Assert
        assertTrue(actual = bridgeClasses(result = first).intersect(bridgeClasses(result = second)).isEmpty())
        assertEquals(expected = CATALOG.version.value, actual = firstVersion)
        assertEquals(expected = SECOND_CATALOG.version.value, actual = secondVersion)
    }

    private fun placedVersion(loader: ClassLoader, callerPackage: String): String =
        loader.loadClass("$callerPackage.$RUNNER_CLASS").getMethod(RUNNER_FUNCTION).invoke(null) as String

    private fun compileRunner(moduleName: String, callerPackage: String, held: Catalog): JvmCompilationResult {
        val result = CompileTestSupport().compileSource(
            fileName = RUNNER_FILE,
            source = runnerSource(callerPackage = callerPackage),
            options = producerOptions(catalog(catalog = held, bridgePackage = BRIDGE_PACKAGE)),
            friendPaths = listOf(producerClasses()),
            moduleName = moduleName,
        )
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
        return result
    }

    private fun producerClasses(): File =
        File(Class.forName(PRODUCER_CLASS).protectionDomain.codeSource.location.toURI())

    private fun runnerSource(callerPackage: String): String =
        // language=kotlin
        """
        @file:JvmName("$RUNNER_CLASS")

        package $callerPackage

        import androidx.compose.runtime.Composition
        import androidx.compose.runtime.Recomposer
        import app.bridge.$COMPONENT
        import dev.tonholo.ssrcompose.producer.internal.PlacedNode
        import dev.tonholo.ssrcompose.producer.internal.ScreenApplier
        import kotlin.coroutines.EmptyCoroutineContext

        fun $RUNNER_FUNCTION(): String {
            val root = PlacedNode.Root()
            val recomposer = Recomposer(EmptyCoroutineContext)
            val composition = Composition(ScreenApplier(root), recomposer)
            composition.setContent { $COMPONENT(title = "text") }
            val placed = root.children.single() as PlacedNode.Component
            composition.dispose()
            recomposer.cancel()
            return placed.catalogVersion.toString()
        }
        """.trimIndent()

    private fun compileModule(moduleName: String): JvmCompilationResult {
        val result = CompileTestSupport().compileSource(
            fileName = CALLER_FILE,
            source = CALLER_SOURCE,
            options = producerOptions(catalog(catalog = CATALOG, bridgePackage = BRIDGE_PACKAGE)),
            moduleName = moduleName,
        )
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
        return result
    }

    private fun bridgeClasses(result: JvmCompilationResult): Set<String> =
        result.outputDirectory.resolve(BRIDGE_FOLDER).listFiles().orEmpty().mapTo(destination = HashSet()) { it.name }

    private companion object {
        const val COMPONENT = "Alpha"
        const val FIRST_MODULE = "module-one"
        const val SECOND_MODULE = "module-two"
        const val BRIDGE_PACKAGE = "app.bridge"
        const val BRIDGE_FOLDER = "app/bridge"
        const val CALLER_FILE = "Screen.kt"
        const val INTERNAL_ERROR = "it is internal"
        const val FIRST_CALLER = "module.one"
        const val SECOND_CALLER = "module.two"
        const val RUNNER_FILE = "Runner.kt"
        const val RUNNER_CLASS = "ModuleRunner"
        const val RUNNER_FUNCTION = "placedVersion"
        const val PRODUCER_CLASS = "dev.tonholo.ssrcompose.producer.internal.ScreenApplier"
        val ALPHA_COMPONENT = ComponentDescriptor(name = COMPONENT, parameters = listOf(TestCatalogs.requiredTitle))
        val CATALOG = TestCatalogs.catalogOf(version = "1", ALPHA_COMPONENT)
        val SECOND_CATALOG = TestCatalogs.catalogOf(version = "2", ALPHA_COMPONENT)

        // language=kotlin
        val CALLER_SOURCE = """
            package module.caller

            import androidx.compose.runtime.Composable
            import app.bridge.$COMPONENT

            @Composable
            fun Screen() {
                $COMPONENT(title = "text")
            }
        """.trimIndent()
    }
}
