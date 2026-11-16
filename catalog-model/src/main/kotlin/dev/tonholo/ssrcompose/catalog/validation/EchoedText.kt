package dev.tonholo.ssrcompose.catalog.validation

internal object EchoedText {
    const val MAX_LENGTH: Int = 64
    const val CUT_MARKER: String = "[cut]"
}

/**
 * The text as it may appear in an error message: the first 64 characters, then a marker when anything was cut.
 */
internal fun String.cutForMessage(): String =
    if (length <= EchoedText.MAX_LENGTH) this else take(n = EchoedText.MAX_LENGTH) + EchoedText.CUT_MARKER
