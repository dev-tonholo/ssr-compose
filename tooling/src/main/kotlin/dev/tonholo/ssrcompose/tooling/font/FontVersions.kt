package dev.tonholo.ssrcompose.tooling.font

import dev.tonholo.ssrcompose.core.font.FontBundleVersion
import dev.tonholo.ssrcompose.tooling.StepException

internal fun fontVersionOf(text: String): FontBundleVersion = try {
    FontBundleVersion(value = text)
} catch (cause: IllegalArgumentException) {
    throw StepException(message = "The font version \"$text\" is not valid: ${cause.message}", cause = cause)
}
