package dev.tonholo.ssrcompose.compiler.producer

import com.tschuchort.compiletesting.JvmCompilationResult
import dev.tonholo.ssrcompose.compiler.testing.ProducerTestOptions.catalog
import dev.tonholo.ssrcompose.compiler.testing.ProducerTestOptions.producerOptions
import dev.tonholo.ssrcompose.compiler.testing.TestCatalogs
import dev.tonholo.ssrcompose.compiler.testing.text.assertWithinOnePixel
import dev.tonholo.ssrcompose.compiler.testing.text.compileTextRunner
import dev.tonholo.ssrcompose.compiler.testing.text.onAStraightScale
import dev.tonholo.ssrcompose.compiler.testing.text.onTheCurve
import dev.tonholo.ssrcompose.compiler.testing.text.probe
import kotlin.test.Test
import kotlin.test.assertEquals

class TextRewriteTest {
    @Test
    fun `given a String call with positional arguments, when rewritten, then it is on the curve and marked`() {
        assertDrawnOnTheCurve(screen = "stringPositional")
    }

    @Test
    fun `given an AnnotatedString call, when rewritten, then it is on the curve and marked`() {
        assertDrawnOnTheCurve(screen = "annotatedString")
    }

    @Test
    fun `given named arguments in another order, when rewritten, then it is on the curve and marked`() {
        assertDrawnOnTheCurve(screen = "namedOutOfOrder")
    }

    @Test
    fun `given defaults left out, when rewritten, then it is on the curve and marked`() {
        assertDrawnOnTheCurve(screen = "defaultsLeftOut")
    }

    @Test
    fun `given a call inside another composable, when rewritten, then it is on the curve and marked`() {
        assertDrawnOnTheCurve(screen = "insideComposable")
    }

    @Test
    fun `given a call inside a lambda, when rewritten, then it is on the curve and marked`() {
        assertDrawnOnTheCurve(screen = "insideLambda")
    }

    @Test
    fun `given a call inside an inline function, when rewritten, then it is on the curve and marked`() {
        assertDrawnOnTheCurve(screen = "insideInlineFunction")
    }

    @Test
    fun `given a module with no catalog, when rewritten in producer mode, then it is on the curve and marked`() {
        // Arrange
        val expected = onTheCurve()

        // Act
        val actual = compiledWithoutCatalog.probe(screen = "stringPositional")

        // Assert
        assertWithinOnePixel(expected = expected, actual = actual)
    }

    @Test
    fun `given no curve provided, when a rewritten call is drawn, then it has no mark and the straight height`() {
        // Arrange
        val expected = onAStraightScale()

        // Act
        val actual = compiled.probe(screen = "stringPositional", withCurve = false)

        // Assert
        assertWithinOnePixel(expected = expected, actual = actual)
    }

    @Test
    fun `given a function reference to BasicText, when rewritten, then the call it adapts into is marked`() {
        // Arrange
        val expected = listOf<Float?>(DEFAULT_SP)

        // Act
        val actual = compiled.probe(screen = "functionReference").map { probe -> probe.markSizeSp }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    private fun assertDrawnOnTheCurve(screen: String) {
        // Arrange
        val expected = onTheCurve()

        // Act
        val actual = compiled.probe(screen = screen)

        // Assert
        assertWithinOnePixel(expected = expected, actual = actual)
    }

    private companion object {
        const val BRIDGE_PACKAGE = "app.bridge.v1"
        const val DEFAULT_SP = 14f

        val compiled: JvmCompilationResult by lazy {
            val bridges = catalog(catalog = TestCatalogs.first, bridgePackage = BRIDGE_PACKAGE)
            compileTextRunner(options = producerOptions(bridges))
        }

        val compiledWithoutCatalog: JvmCompilationResult by lazy { compileTextRunner(options = producerOptions()) }
    }
}
