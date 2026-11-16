package dev.tonholo.ssrcompose.catalog.validation

internal object EchoedText {
    const val MAX_LENGTH: Int = 64
    const val CUT_MARKER: String = "[cut]"
}

/**
 * The text as it may appear in an error message: the first 64 characters, then a marker when anything was cut.
 * Names and values from a catalog file are not trusted, so a message never holds all of a long one.
 */
internal fun String.cutForMessage(): String =
    if (length <= EchoedText.MAX_LENGTH) this else take(n = EchoedText.MAX_LENGTH) + EchoedText.CUT_MARKER
