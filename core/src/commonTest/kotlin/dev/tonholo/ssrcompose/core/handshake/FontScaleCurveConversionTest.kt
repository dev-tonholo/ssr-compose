package dev.tonholo.ssrcompose.core.handshake

import kotlin.math.round
import kotlin.test.Test
import kotlin.test.assertEquals

class FontScaleCurveConversionTest {
    @Test
    fun `given the emulator's curve, when sp sizes are converted, then points match and gaps are interpolated`() {
        // Arrange
        val curve = FontScaleCurve(points = EMULATOR_POINTS)
        val sizes = listOf(12f, 16f, 24f, 14f, 20f)
        val expected = listOf(15.6f, 20.2f, 26.4f, 17.9f, 23.3f)

        // Act
        val actual = sizes.map { sp -> curve.spToDp(sp = sp) }

        // Assert
        assertEquals(expected = expected, actual = actual.map { dp -> dp.rounded() })
    }

    @Test
    fun `given the emulator's curve, when sizes outside it are converted, then the end points' ratios apply`() {
        // Arrange
        val curve = FontScaleCurve(points = EMULATOR_POINTS)
        val sizes = listOf(6f, 48f)
        val expected = listOf(6f * FIRST_RATIO, 48f * LAST_RATIO)

        // Act
        val actual = sizes.map { sp -> curve.spToDp(sp = sp) }

        // Assert
        assertEquals(expected = expected.map { dp -> dp.rounded() }, actual = actual.map { dp -> dp.rounded() })
    }

    @Test
    fun `given a linear curve at font scale 130 percent, when any sp size is converted, then it is scaled the same`() {
        // Arrange
        val points = FontScaleCurve.SAMPLED_SIZES.map { sp -> FontScalePoint(sp = sp, dp = sp * LINEAR_SCALE) }
        val curve = FontScaleCurve(points = points)
        val sizes = listOf(4f, 8f, 11f, 13f, 27f, 100f, 300f)

        // Act
        val actual = sizes.map { sp -> curve.spToDp(sp = sp).rounded() }

        // Assert
        assertEquals(expected = sizes.map { sp -> (sp * LINEAR_SCALE).rounded() }, actual = actual)
    }

    private fun Float.rounded(): Float = round(x = this * ROUNDING) / ROUNDING

    private companion object {
        const val LINEAR_SCALE = 1.3f
        const val FIRST_RATIO = 15.6f / 12f
        const val LAST_RATIO = 26.4f / 24f
        const val ROUNDING = 100f
        val EMULATOR_POINTS = listOf(
            FontScalePoint(sp = 12f, dp = 15.6f),
            FontScalePoint(sp = 16f, dp = 20.2f),
            FontScalePoint(sp = 24f, dp = 26.4f),
        )
    }
}
