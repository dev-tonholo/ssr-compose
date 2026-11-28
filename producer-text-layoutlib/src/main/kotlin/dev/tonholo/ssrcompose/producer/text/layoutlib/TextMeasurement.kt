package dev.tonholo.ssrcompose.producer.text.layoutlib

/**
 * The laid out text, in px from its top. [widthPx] is the width of its box. [lineEnds] holds, for each line, the
 * index in the text where Compose ends its visible characters, or where an ellipsis cuts it. [lineRightsPx] holds
 * each line's right edge and [lineBottomsPx] its bottom.
 */
internal data class TextMeasurement(
    val heightPx: Int,
    val widthPx: Int,
    val lineEnds: List<Int>,
    val lineRightsPx: List<Float>,
    val lineBottomsPx: List<Int>,
    val isLastLineEllipsized: Boolean,
    val firstBaselinePx: Int,
    val lastBaselinePx: Int,
) {
    val lineCount: Int
        get() = lineEnds.size
}
