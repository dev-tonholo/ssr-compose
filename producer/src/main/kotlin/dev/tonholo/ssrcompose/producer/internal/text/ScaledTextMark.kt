package dev.tonholo.ssrcompose.producer.internal.text

import androidx.compose.ui.semantics.SemanticsPropertyKey

/**
 * What `ScaledBasicText` wrote for a text: its font size and line height in sp, as the server wrote them. The line
 * height is null when the style has none.
 */
internal data class ScaledTextMark(val sizeSp: Float, val lineHeightSp: Float?)

internal val scaledTextMark: SemanticsPropertyKey<ScaledTextMark> =
    SemanticsPropertyKey(name = "ScaledTextMark")
