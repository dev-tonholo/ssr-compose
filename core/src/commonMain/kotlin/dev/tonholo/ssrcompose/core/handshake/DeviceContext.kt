package dev.tonholo.ssrcompose.core.handshake

import kotlinx.serialization.Serializable

private const val MAX_FONT_SCALE = 4f

/**
 * What the producer may know about the device. It can change during a session.
 * [orientation] is derived from the size. The producer measures text with [fontScale] and lays out in
 * [layoutDirection].
 *
 * @throws IllegalArgumentException when [fontScale] is not above 0 or is above 4.
 */
@Serializable
public data class DeviceContext(
    val density: Float,
    val screenWidthPx: Int,
    val screenHeightPx: Int,
    val theme: Theme,
    val language: String,
    val fontScale: Float,
    val layoutDirection: LayoutDirection,
) {
    init {
        require(fontScale > 0f && fontScale <= MAX_FONT_SCALE) {
            "A font scale must be above 0 and at most $MAX_FONT_SCALE, was $fontScale"
        }
    }

    val orientation: Orientation
        get() = if (screenWidthPx > screenHeightPx) Orientation.Landscape else Orientation.Portrait
}
