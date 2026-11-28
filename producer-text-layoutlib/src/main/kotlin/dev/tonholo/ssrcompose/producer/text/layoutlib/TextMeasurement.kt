package dev.tonholo.ssrcompose.producer.text.layoutlib

/**
 * The laid out text, in px from its top. [lineEnds] holds, for each line, the index in the text where Compose ends
 * its visible characters, or where an ellipsis cuts it.
 */
internal data class TextMeasurement(
    val heightPx: Int,
    val lineEnds: List<Int>,
    val firstBaselinePx: Int,
    val lastBaselinePx: Int,
) {
    val lineCount: Int
        get() = lineEnds.size
}
