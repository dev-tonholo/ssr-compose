package dev.tonholo.ssrcompose.producer.internal.reader

/**
 * Why the producer cannot send what a screen shows. [description] completes "The screen holds ...".
 */
internal enum class NotSendableReason(val description: String) {
    Image(description = "an image"),
    Brush(description = "a brush, such as a gradient, as a background or border"),
    LazyList(description = "a lazy list, which composes only its visible items; use verticalScroll"),
    UnknownShape(description = "a shape other than a rectangle or rounded corners"),
    NoFontFamily(description = "a text with no font family or with FontFamily.Default; name the bundled Roboto family"),
    UnknownFontFamily(description = "a text in a font family that is not the bundled Roboto"),
    SeveralStyles(description = "a text with several styles"),
    TextWithDecoration(description = "a text with its own background or border"),
    ScrollWithDecoration(description = "a scroll container with its own background or border"),
    DecorationsApart(description = "a background and a border with different boxes or shapes, or two of either"),
    GraphicsLayer(description = "a graphics layer that scales, rotates, moves, fades, clips or adds a shadow"),
    RelativeFontSize(description = "a text with a font size that is not in sp, such as em"),
    TextField(description = "a text field"),
    UnsupportedTextStyle(
        description = "a text with letter spacing, a decoration, a brush, a shadow, a baseline shift, an indent, " +
            "no soft wrap, or an overflow other than clip or ellipsis",
    ),
}
