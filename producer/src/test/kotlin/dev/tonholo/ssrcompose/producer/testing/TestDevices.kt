package dev.tonholo.ssrcompose.producer.testing

import dev.tonholo.ssrcompose.core.handshake.DeviceContext
import dev.tonholo.ssrcompose.core.handshake.FontScaleCurve
import dev.tonholo.ssrcompose.core.handshake.FontScalePoint
import dev.tonholo.ssrcompose.core.handshake.LayoutDirection
import dev.tonholo.ssrcompose.core.handshake.Theme

internal const val TEST_DENSITY = 2.5f
internal const val TEST_FONT_SCALE = 1.3f
private const val TEST_WIDTH_PX = 1000
private const val TEST_HEIGHT_PX = 2000
private const val TEST_LANGUAGE = "en-US"

internal fun linearCurve(scale: Float): FontScaleCurve =
    FontScaleCurve(points = FontScaleCurve.SAMPLED_SIZES.map { sp -> FontScalePoint(sp = sp, dp = sp * scale) })

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
