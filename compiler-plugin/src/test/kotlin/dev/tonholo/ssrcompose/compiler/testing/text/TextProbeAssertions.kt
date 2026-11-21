package dev.tonholo.ssrcompose.compiler.testing.text

import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Heights in pixels are the probes of ADR 0023 at density 2.5 and font scale 1.3: Skia's own metrics for the bundled
 * Roboto give 59 and 77 px for 16 and 24 sp on the emulator curve, and 61 and 91 px on a straight font scale.
 */
private const val CURVE_HEIGHT_16_SP = 59
private const val CURVE_HEIGHT_24_SP = 77
private const val STRAIGHT_HEIGHT_16_SP = 61
private const val STRAIGHT_HEIGHT_24_SP = 91
private const val SMALL_SP = 16f
private const val LARGE_SP = 24f
private const val ONE_PX = 1

/** The 16 and 24 sp texts of the runner fixture as the device's curve lays them out, each with its mark. */
internal fun onTheCurve(): List<TextProbe> = listOf(
    TextProbe(heightPx = CURVE_HEIGHT_16_SP, markSizeSp = SMALL_SP),
    TextProbe(heightPx = CURVE_HEIGHT_24_SP, markSizeSp = LARGE_SP),
)

/** The same texts as a straight font scale lays them out, with no mark. */
internal fun onAStraightScale(): List<TextProbe> = listOf(
    TextProbe(heightPx = STRAIGHT_HEIGHT_16_SP, markSizeSp = null),
    TextProbe(heightPx = STRAIGHT_HEIGHT_24_SP, markSizeSp = null),
)

/** Asserts the same marks and heights that differ by at most one pixel, which Skia's rounding allows. */
internal fun assertWithinOnePixel(expected: List<TextProbe>, actual: List<TextProbe>) {
    val message = "expected $expected but was $actual"
    val marks = { probes: List<TextProbe> -> probes.map { probe -> probe.markSizeSp } }
    assertEquals(expected = marks(expected), actual = marks(actual), message = message)
    assertEquals(expected = expected.size, actual = actual.size, message = message)
    expected.zip(actual).forEach { (want, got) ->
        assertTrue(actual = abs(want.heightPx - got.heightPx) <= ONE_PX, message = message)
    }
}
