package dev.tonholo.ssrcompose.producer.testing

import androidx.compose.ui.unit.Density
import dev.tonholo.ssrcompose.core.handshake.DeviceContext
import dev.tonholo.ssrcompose.core.handshake.FontScaleCurve
import dev.tonholo.ssrcompose.core.handshake.FontScalePoint
import dev.tonholo.ssrcompose.core.handshake.LayoutDirection
import dev.tonholo.ssrcompose.core.handshake.Theme
import dev.tonholo.ssrcompose.producer.internal.text.ScaledTextScene

internal const val TEST_DENSITY = 2.5f
internal const val TEST_FONT_SCALE = 1.3f
private const val TEST_WIDTH_PX = 1000
private const val TEST_HEIGHT_PX = 2000
private const val TEST_LANGUAGE = "en-US"

internal const val EMULATOR_FONT_SCALE = 1.3f
private const val EMULATOR_LARGE_RATIO = 1.1f
internal const val BAND_FONT_SCALE = 1.04f
private const val BAND_LARGE_RATIO = 1.02f
private const val BAND_RATIO_CHANGE_SP = 24f

internal const val LIVE_FONT_SCALE = 1f

internal fun linearCurve(scale: Float): FontScaleCurve =
    FontScaleCurve(points = FontScaleCurve.SAMPLED_SIZES.map { sp -> FontScalePoint(sp = sp, dp = sp * scale) })

internal fun liveScaledTextScene(): () -> ScaledTextScene = {
    ScaledTextScene(curve = linearCurve(scale = LIVE_FONT_SCALE), fontScale = LIVE_FONT_SCALE)
}

/**
 * The emulator's curve at font scale 1.3 on API 36. 12, 16 and 24 sp are the sizes it drew as 15.6, 20.2 and 26.4 dp
 * (ADR 0023, "Context"). 14 and 18 sp are the points of Compose's own table that put 16 sp at 20.2 dp, 20 sp is the
 * straight line between 18 and 24 sp, and the rest follow the ratios the ADR's probes used: 1.3 below 12 sp and the
 * ratio of 24 sp above it.
 */
internal val EMULATOR_CURVE: FontScaleCurve = FontScaleCurve(
    points = listOf(
        FontScalePoint(sp = 8f, dp = 8f * EMULATOR_FONT_SCALE),
        FontScalePoint(sp = 10f, dp = 10f * EMULATOR_FONT_SCALE),
        FontScalePoint(sp = 12f, dp = 15.6f),
        FontScalePoint(sp = 14f, dp = 18.8f),
        FontScalePoint(sp = 18f, dp = 21.6f),
        FontScalePoint(sp = 20f, dp = 23.2f),
        FontScalePoint(sp = 24f, dp = 26.4f),
        FontScalePoint(sp = 30f, dp = 30f * EMULATOR_LARGE_RATIO),
        FontScalePoint(sp = 100f, dp = 100f * EMULATOR_LARGE_RATIO),
    ),
)

/**
 * A curve at font scale 1.04, where the ratio of 24 sp (1.02) differs from the ratio of 16 sp (1.04), so a line
 * height laid out with the factor of 16 sp differs from one laid out at `curve(24)`.
 */
internal val BAND_CURVE: FontScaleCurve = FontScaleCurve(
    points = FontScaleCurve.SAMPLED_SIZES.map { sp ->
        val ratio = if (sp >= BAND_RATIO_CHANGE_SP) BAND_LARGE_RATIO else BAND_FONT_SCALE
        FontScalePoint(sp = sp, dp = sp * ratio)
    },
)

internal fun testDevice(
    density: Float = TEST_DENSITY,
    fontScale: Float = TEST_FONT_SCALE,
    fontScaleCurve: FontScaleCurve = linearCurve(scale = fontScale),
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
): DeviceContext = DeviceContext(
    density = density,
    screenWidthPx = TEST_WIDTH_PX,
    screenHeightPx = TEST_HEIGHT_PX,
    theme = Theme.Light,
    language = TEST_LANGUAGE,
    fontScale = fontScale,
    fontScaleCurve = fontScaleCurve,
    layoutDirection = layoutDirection,
)

internal val DeviceContext.composeDensity: Density
    get() = Density(density = density, fontScale = fontScale)
