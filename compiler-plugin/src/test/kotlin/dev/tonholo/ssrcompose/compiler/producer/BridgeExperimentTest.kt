package dev.tonholo.ssrcompose.compiler.producer

import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import dev.tonholo.ssrcompose.catalog.options.CatalogOptionValue
import dev.tonholo.ssrcompose.catalog.options.CompilerPluginMode
import dev.tonholo.ssrcompose.catalog.options.CompilerPluginOptions
import dev.tonholo.ssrcompose.compiler.testing.CompileTestSupport
import dev.tonholo.ssrcompose.compiler.testing.ThrowawayIndexFiles
import kotlin.test.Test
import kotlin.test.assertEquals

class BridgeExperimentTest {
    private val support = CompileTestSupport()

    @Test
    fun `given a one-component index, when a caller composes the bridge function, then it compiles`() {
        // Arrange
        val options = producerOptions(
            catalog(indexName = FIRST_INDEX, version = FIRST_VERSION, bridgePackage = FIRST_PACKAGE),
        )

        // Act
        val result = support.compile(fixtureName = GREETING_CALLER, options = options)

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
    }

    private fun catalog(indexName: String, version: String, bridgePackage: String): CatalogOptionValue =
        CatalogOptionValue(
            version = version,
            indexPath = ThrowawayIndexFiles.path(indexName),
            bridgePackage = bridgePackage,
        )

    private fun producerOptions(vararg catalogs: CatalogOptionValue): List<Pair<String, String>> =
        listOf(CompilerPluginOptions.MODE to CompilerPluginMode.Producer.optionValue) +
            catalogs.map { CompilerPluginOptions.CATALOG to it.encode() }

    private companion object {
        const val FIRST_INDEX = "first-catalog.throwaway-index"
        const val FIRST_VERSION = "1"
        const val FIRST_PACKAGE = "app.bridge.v1"
        const val GREETING_CALLER = "GreetingBridgeCaller"
    }
}
