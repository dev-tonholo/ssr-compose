package dev.tonholo.ssrcompose.compiler.producer

import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import dev.tonholo.ssrcompose.compiler.testing.CompileTestSupport
import dev.tonholo.ssrcompose.compiler.testing.ProducerTestOptions.catalog
import dev.tonholo.ssrcompose.compiler.testing.ProducerTestOptions.producerOptions
import kotlin.test.Test
import kotlin.test.assertEquals

class UnplaceableComponentTest {
    private val support = CompileTestSupport()
    private val options = producerOptions(
        catalog(indexName = CATALOG_INDEX, version = CATALOG_VERSION, bridgePackage = BRIDGE_PACKAGE),
    )

    @Test
    fun `given a placeable and an unplaceable component, when a caller uses the placeable one, then it compiles`() {
        // Arrange
        val fixture = PLACEABLE_CALLER

        // Act
        val result = support.compile(fixtureName = fixture, options = options)

        // Assert
        assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
    }

    private companion object {
        const val CATALOG_INDEX = "unplaceable-catalog.throwaway-index"
        const val CATALOG_VERSION = "3"
        const val BRIDGE_PACKAGE = "app.bridge.unplaceable"
        const val PLACEABLE_CALLER = "PlaceableNextToUnplaceableCaller"
    }
}
