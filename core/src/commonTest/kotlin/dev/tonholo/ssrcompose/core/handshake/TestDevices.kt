package dev.tonholo.ssrcompose.core.handshake

/** The devices core's tests introduce, built in one place. */
internal object TestDevices {
    const val DENSITY = 2.5f
    const val FONT_SCALE = 1.3f

    val portrait = DeviceContext(
        density = DENSITY,
        screenWidthPx = 1080,
        screenHeightPx = 2400,
        theme = Theme.Light,
        language = "en-US",
        fontScale = FONT_SCALE,
        layoutDirection = LayoutDirection.Ltr,
    )
}
