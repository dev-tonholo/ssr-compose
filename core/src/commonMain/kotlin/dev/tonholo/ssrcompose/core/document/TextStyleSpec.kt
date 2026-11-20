package dev.tonholo.ssrcompose.core.document

import dev.tonholo.ssrcompose.core.font.FONT_WEIGHTS
import kotlinx.serialization.Serializable

private const val MAX_SIZE = 1_000f

/**
 * How a text is drawn. A null [family] is the library's own Roboto. [size] and [lineHeight] are in sp.
 *
 * @throws IllegalArgumentException when [size] or [lineHeight] is not above 0 or is above 1,000, or [weight] is
 * outside 1 to 1,000.
 */
@Serializable
public data class TextStyleSpec(
    val family: String? = null,
    val size: Float,
    val weight: Int,
    val italic: Boolean = false,
    val color: DocumentColor,
    val lineHeight: Float? = null,
    val align: Align = Align.Start,
) {
    init {
        requireTextSize(value = size, what = "font size")
        lineHeight?.let { requireTextSize(value = it, what = "line height") }
        require(weight in FONT_WEIGHTS) { "A weight must be in $FONT_WEIGHTS, was $weight" }
    }

    @Serializable
    public enum class Align {
        Start,
        Center,
        End,
        Justify,
    }
}

private fun requireTextSize(value: Float, what: String) {
    require(value > 0f && value <= MAX_SIZE) { "A $what must be above 0 and at most $MAX_SIZE, was $value" }
}
