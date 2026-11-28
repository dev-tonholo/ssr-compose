package dev.tonholo.ssrcompose.producer.text.layoutlib

/**
 * A line height the text style gives in sp: [sp] is that value and [px] the same value in px. [fontScale] is the
 * device's font scale.
 *
 * The request's [TextRequest.sizePx], [px] and [sizePxAsSp] come from `Density.toPx()` and `Density.toSp()` on
 * Android's density with the device's font scale curve. Never convert any of them linearly, as desktop Compose does by
 * default: on a device with such a curve that gives wrong values.
 *
 * [sizePxAsSp] is the request's font size in px converted back to sp: the px divided by the density, then dp to sp
 * through the curve, in 32 bit floats. It is not the style's font size. The round trip is not exact, and above font
 * scale 1.05 that can move each line by 1 px.
 *
 * @throws IllegalArgumentException when a value is not finite, a height is negative, or [sizePxAsSp] or [fontScale]
 * is not above 0.
 */
internal data class TextLineHeight(
    val sp: Float,
    val px: Float,
    val sizePxAsSp: Float,
    val fontScale: Float,
) {
    init {
        require(sp.isFinite() && sp >= 0f) { "sp must be 0 or more, not $sp" }
        require(px.isFinite() && px >= 0f) { "px must be 0 or more, not $px" }
        require(sizePxAsSp.isFinite() && sizePxAsSp > 0f) { "sizePxAsSp must be above 0, not $sizePxAsSp" }
        require(fontScale.isFinite() && fontScale > 0f) { "fontScale must be above 0, not $fontScale" }
    }
}
