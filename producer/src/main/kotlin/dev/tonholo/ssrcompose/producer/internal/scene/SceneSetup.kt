package dev.tonholo.ssrcompose.producer.internal.scene

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import dev.tonholo.ssrcompose.core.document.Viewport
import dev.tonholo.ssrcompose.core.handshake.DeviceContext
import kotlin.math.roundToInt
import dev.tonholo.ssrcompose.core.handshake.LayoutDirection as DeviceLayoutDirection

private const val MIN_SIDE_PX = 1

/**
 * What a screen's scene is created with: its size in pixels, its density and its layout direction.
 */
internal data class SceneSetup(
    val widthPx: Int,
    val heightPx: Int,
    val density: Density,
    val layoutDirection: LayoutDirection,
)

/**
 * The scene of a screen shown in [viewport] on [device].
 */
internal fun sceneSetup(viewport: Viewport, device: DeviceContext): SceneSetup = SceneSetup(
    widthPx = viewport.width.toPixels(density = device.density),
    heightPx = viewport.height.toPixels(density = device.density),
    density = sceneDensity(device = device),
    layoutDirection = device.layoutDirection.toCompose(),
)

private fun Float.toPixels(density: Float): Int =
    (this * density).roundToInt().coerceAtLeast(minimumValue = MIN_SIDE_PX)

private fun DeviceLayoutDirection.toCompose(): LayoutDirection = when (this) {
    DeviceLayoutDirection.Ltr -> LayoutDirection.Ltr
    DeviceLayoutDirection.Rtl -> LayoutDirection.Rtl
}
