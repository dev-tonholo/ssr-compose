package dev.tonholo.ssrcompose.producer.internal.text

import dev.tonholo.ssrcompose.core.handshake.FontScaleCurve

/**
 * The device's font scale [curve] and its [fontScale], the font scale the device's layout of line heights follows. A
 * text takes both from its screen's scene and never from the `LocalDensity` around it, which server code may change.
 */
internal data class ScaledTextScene(val curve: FontScaleCurve, val fontScale: Float)
