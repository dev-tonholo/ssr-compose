package dev.tonholo.ssrcompose.core.handshake

import kotlinx.serialization.Serializable

/**
 * What the producer may know about the device. It can change during a session.
 * [orientation] is derived from the size.
 */
@Serializable
public data class DeviceContext(
    val density: Float,
    val screenWidthPx: Int,
    val screenHeightPx: Int,
    val theme: Theme,
    val language: String,
) {
    val orientation: Orientation
        get() = if (screenWidthPx > screenHeightPx) Orientation.Landscape else Orientation.Portrait
}
