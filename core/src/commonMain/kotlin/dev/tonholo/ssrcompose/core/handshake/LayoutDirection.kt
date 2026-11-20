package dev.tonholo.ssrcompose.core.handshake

import kotlinx.serialization.Serializable

/** The direction the device lays text and screens out in: left to right or right to left. */
@Serializable
public enum class LayoutDirection {
    Ltr,
    Rtl,
}
