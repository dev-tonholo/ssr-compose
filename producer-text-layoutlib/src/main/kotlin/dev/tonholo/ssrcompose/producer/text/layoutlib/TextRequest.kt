package dev.tonholo.ssrcompose.producer.text.layoutlib

/**
 * One text to measure: [sizePx] is the font size and [widthPx] the width the text may use.
 *
 * @throws IllegalArgumentException when [sizePx] is negative or not finite.
 */
internal data class TextRequest(
    val text: String,
    val font: TextFont,
    val sizePx: Float,
    val widthPx: Int,
) {
    init {
        require(sizePx.isFinite() && sizePx >= 0f) { "sizePx must be 0 or more, not $sizePx" }
    }
}
