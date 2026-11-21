package dev.tonholo.ssrcompose.core.handshake

import kotlinx.serialization.Serializable

/**
 * One point of the device's font scale curve: a text size of [sp] takes [dp] on the device.
 */
@Serializable
public data class FontScalePoint(val sp: Float, val dp: Float)
