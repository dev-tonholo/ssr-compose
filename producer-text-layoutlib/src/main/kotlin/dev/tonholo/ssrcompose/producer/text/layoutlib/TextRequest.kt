package dev.tonholo.ssrcompose.producer.text.layoutlib

/**
 * One text to measure: [sizePx] is the font size, [widthPx] the width the text may use and [lineHeightPx] the height
 * of each line, or null for the font's own.
 *
 * @throws IllegalArgumentException when [sizePx] is negative or not finite, or [maxLines] is less than 1.
 */
internal data class TextRequest(
    val text: String,
    val font: TextFont,
    val sizePx: Float,
    val widthPx: Int,
    val lineHeightPx: Float? = null,
    val maxLines: Int = Int.MAX_VALUE,
    val overflow: TextOverflow = TextOverflow.Clip,
) {
    init {
        require(sizePx.isFinite() && sizePx >= 0f) { "sizePx must be 0 or more, not $sizePx" }
        require(maxLines >= 1) { "maxLines must be 1 or more, not $maxLines" }
    }
}
