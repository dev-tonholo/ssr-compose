package dev.tonholo.ssrcompose.producer.internal.scene

import androidx.compose.ui.unit.Density
import dev.tonholo.ssrcompose.core.handshake.DeviceContext

/**
 * The density a screen's scene measures with: the device's pixels per dp and its font scale.
 */
internal fun sceneDensity(device: DeviceContext): Density =
    Density(density = device.density, fontScale = device.fontScale)
