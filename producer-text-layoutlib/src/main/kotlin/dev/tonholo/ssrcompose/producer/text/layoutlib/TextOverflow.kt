package dev.tonholo.ssrcompose.producer.text.layoutlib

/** What happens to the text past the last line that [TextRequest.maxLines] allows. */
internal enum class TextOverflow {
    Clip,
    Ellipsis,
}
