package dev.tonholo.ssrcompose.core.handshake

/** The devices core's tests introduce, built in one place. */
internal object TestDevices {
    const val DENSITY = 2.5f
    const val FONT_SCALE = 1.3f

    fun linearCurve(scale: Float): FontScaleCurve =
        FontScaleCurve(points = FontScaleCurve.SAMPLED_SIZES.map { sp -> FontScalePoint(sp = sp, dp = sp * scale) })

    val portrait = DeviceContext(
        density = DENSITY,
        screenWidthPx = 1080,
        screenHeightPx = 2400,
        theme = Theme.Light,
        language = "en-US",
        fontScale = FONT_SCALE,
        fontScaleCurve = linearCurve(scale = FONT_SCALE),
        layoutDirection = LayoutDirection.Ltr,
    )
}
