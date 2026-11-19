package dev.tonholo.ssrcompose.compiler.producer

import androidx.compose.compiler.plugins.kotlin.ComposePluginRegistrar
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.compiler.SsrCompilerPluginRegistrar
import dev.tonholo.ssrcompose.compiler.testing.CalledBridgeOptIns
import dev.tonholo.ssrcompose.compiler.testing.CompileTestSupport
import dev.tonholo.ssrcompose.compiler.testing.ProducerTestOptions.catalog
import dev.tonholo.ssrcompose.compiler.testing.ProducerTestOptions.producerOptions
import dev.tonholo.ssrcompose.compiler.testing.TestCatalogs
import dev.tonholo.ssrcompose.producer.InternalSsrComposeApi
import kotlin.test.Test
import kotlin.test.assertEquals
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

class InternalSsrComposeApiTest {
    @Test
    fun `given hand calls to every bridge entry point without opt-in, when compiled, then each one warns`() {
        // Arrange
        val expectedWarnings = BRIDGE_ENTRY_POINTS

        // Act
        val result = CompileTestSupport(registrars = listOf(ComposePluginRegistrar())).compileSource(
            fileName = HAND_BRIDGE_FILE,
            source = HAND_BRIDGE_SOURCE,
        )

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
        assertEquals(
            expected = expectedWarnings,
            actual = optInWarnings(messages = result.messages).size,
            message = result.messages,
        )
    }

    @Test
    fun `given a server screen calling a created bridge, when compiled, then it needs no opt-in`() {
        // Arrange
        val expectedWarnings = emptyList<String>()

        // Act
        val result = CompileTestSupport().compileSource(
            fileName = SCREEN_FILE,
            source = SCREEN_SOURCE,
            options = producerOptions(catalog(catalog = CATALOG, bridgePackage = BRIDGE_PACKAGE)),
        )

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
        assertEquals(
            expected = expectedWarnings,
            actual = optInWarnings(messages = result.messages),
            message = result.messages,
        )
    }

    @Test
    fun `given a server screen calling a created bridge, when the bridge declaration is read, then it opts in`() {
        // Arrange
        val expected = listOf(listOf(INTERNAL_API_MARKER))
        val calledBridgeOptIns = CalledBridgeOptIns()

        // Act
        val result = CompileTestSupport(
            registrars = listOf(SsrCompilerPluginRegistrar(), ComposePluginRegistrar(), calledBridgeOptIns),
        ).compileSource(
            fileName = SCREEN_FILE,
            source = SCREEN_SOURCE,
            options = producerOptions(catalog(catalog = CATALOG, bridgePackage = BRIDGE_PACKAGE)),
        )

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
        assertEquals(expected = expected, actual = calledBridgeOptIns.markersPerCall)
    }

    private fun optInWarnings(messages: String): List<String> =
        messages.lines().filter { line -> line.startsWith(WARNING_PREFIX) && OPT_IN_MARKER in line }

    private companion object {
        const val BRIDGE_ENTRY_POINTS = 7
        const val WARNING_PREFIX = "w:"
        const val OPT_IN_MARKER = "InternalSsrComposeApi"
        const val HAND_BRIDGE_FILE = "HandBridge.kt"
        const val SCREEN_FILE = "Screen.kt"
        const val COMPONENT = "Alpha"
        const val BRIDGE_PACKAGE = "app.bridge"
        val INTERNAL_API_MARKER = ClassId.topLevel(FqName(checkNotNull(InternalSsrComposeApi::class.qualifiedName)))
        val CATALOG = TestCatalogs.catalogOf(
            version = "1",
            ComponentDescriptor(name = COMPONENT, parameters = listOf(TestCatalogs.requiredTitle)),
        )

        // language=kotlin
        val HAND_BRIDGE_SOURCE = """
            package app.hand

            import androidx.compose.runtime.Composable
            import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
            import dev.tonholo.ssrcompose.producer.ConsumerComponent
            import dev.tonholo.ssrcompose.producer.UnplaceableBridge
            import dev.tonholo.ssrcompose.producer.choiceListValue
            import dev.tonholo.ssrcompose.producer.decimalListValue
            import dev.tonholo.ssrcompose.producer.flagListValue
            import dev.tonholo.ssrcompose.producer.textListValue
            import dev.tonholo.ssrcompose.producer.wholeListValue

            enum class Tone { Calm }

            @Composable
            fun Tags(tags: List<String>, counts: List<Long>, sizes: List<Double>, marks: List<Boolean>) {
                ConsumerComponent(
                    catalogVersion = CatalogVersion(value = "1"),
                    name = "Tags",
                    values = mapOf(
                        "tags" to textListValue(items = tags),
                        "counts" to wholeListValue(items = counts),
                        "sizes" to decimalListValue(items = sizes),
                        "marks" to flagListValue(items = marks),
                        "tones" to choiceListValue(items = listOf(Tone.Calm)),
                    ),
                )
            }

            @UnplaceableBridge(message = "not placeable")
            @Composable
            fun Scanner() = Unit
        """.trimIndent()

        // language=kotlin
        val SCREEN_SOURCE = """
            package app.screen

            import androidx.compose.runtime.Composable
            import app.bridge.$COMPONENT

            @Composable
            fun Screen() {
                $COMPONENT(title = "text")
            }
        """.trimIndent()
    }
}
