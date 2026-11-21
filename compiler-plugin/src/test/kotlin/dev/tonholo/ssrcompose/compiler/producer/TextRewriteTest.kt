package dev.tonholo.ssrcompose.compiler.producer

import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode
import dev.tonholo.ssrcompose.compiler.testing.CompileTestSupport
import dev.tonholo.ssrcompose.compiler.testing.ProducerTestOptions.catalog
import dev.tonholo.ssrcompose.compiler.testing.ProducerTestOptions.producerOptions
import dev.tonholo.ssrcompose.compiler.testing.TestCatalogs
import dev.tonholo.ssrcompose.compiler.testing.text.TextProbe
import dev.tonholo.ssrcompose.compiler.testing.text.probe
import dev.tonholo.ssrcompose.compiler.testing.text.producerClasses
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Heights in pixels are the probes of ADR 0023 at density 2.5 and font scale 1.3: Skia's own metrics for the bundled
 * Roboto give 59 and 77 px for 16 and 24 sp on the emulator curve, and 61 and 91 px on a straight font scale.
 */
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
        val expected = listOf(
            TextProbe(heightPx = STRAIGHT_HEIGHT_16_SP, markSizeSp = null),
            TextProbe(heightPx = STRAIGHT_HEIGHT_24_SP, markSizeSp = null),
        )

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

    private fun onTheCurve() = listOf(
        TextProbe(heightPx = CURVE_HEIGHT_16_SP, markSizeSp = SMALL_SP),
        TextProbe(heightPx = CURVE_HEIGHT_24_SP, markSizeSp = LARGE_SP),
    )

    private fun assertDrawnOnTheCurve(screen: String) {
        // Arrange
        val expected = onTheCurve()

        // Act
        val actual = compiled.probe(screen = screen)

        // Assert
        assertWithinOnePixel(expected = expected, actual = actual)
    }

    private fun assertWithinOnePixel(expected: List<TextProbe>, actual: List<TextProbe>) {
        val message = "expected $expected but was $actual"
        val marks = { probes: List<TextProbe> -> probes.map { probe -> probe.markSizeSp } }
        assertEquals(expected = marks(expected), actual = marks(actual), message = message)
        assertEquals(expected = expected.size, actual = actual.size, message = message)
        expected.zip(actual).forEach { (want, got) ->
            assertTrue(actual = abs(want.heightPx - got.heightPx) <= ONE_PX, message = message)
        }
    }

    private companion object {
        const val RUNNER_FIXTURE = "TextRewriteRunner"
        const val BRIDGE_PACKAGE = "app.bridge.v1"
        const val CURVE_HEIGHT_16_SP = 59
        const val CURVE_HEIGHT_24_SP = 77
        const val STRAIGHT_HEIGHT_16_SP = 61
        const val STRAIGHT_HEIGHT_24_SP = 91
        const val SMALL_SP = 16f
        const val LARGE_SP = 24f
        const val DEFAULT_SP = 14f
        const val ONE_PX = 1

        val compiled: JvmCompilationResult by lazy {
            val bridges = catalog(catalog = TestCatalogs.first, bridgePackage = BRIDGE_PACKAGE)
            compileRunner(options = producerOptions(bridges))
        }

        val compiledWithoutCatalog: JvmCompilationResult by lazy { compileRunner(options = producerOptions()) }

        private fun compileRunner(options: List<Pair<String, String>>): JvmCompilationResult =
            CompileTestSupport().compile(
                fixtureName = RUNNER_FIXTURE,
                options = options,
                friendPaths = listOf(producerClasses()),
            ).also { result ->
                assertEquals(expected = ExitCode.OK, actual = result.exitCode, message = result.messages)
            }
    }
}
