package dev.tonholo.ssrcompose.core.label

private const val MAX_LENGTH = 64
private const val DOT = '.'
private const val DEVICE_NUMBER_FIRST = 1
private const val DEVICE_NUMBER_LAST = 9
private val ALLOWED_PUNCTUATION = setOf(DOT, '-', '_')
private val BASE_DEVICE_NAMES = setOf("con", "prn", "aux", "nul")
private val NUMBERED_DEVICE_PREFIXES = setOf("com", "lpt")

private val DEVICE_NAMES: Set<String> = BASE_DEVICE_NAMES + NUMBERED_DEVICE_PREFIXES.flatMap { prefix ->
    (DEVICE_NUMBER_FIRST..DEVICE_NUMBER_LAST).map { number -> "$prefix$number" }
}

/**
 * The rule for a label that names a folder: 1 to 64 characters of lowercase ASCII letters, digits, dots, hyphens
 * and underscores, that neither starts nor ends with a dot, and that is never a Windows device name (`con`,
 * `prn`, `aux`, `nul`, `com1` to `com9`, `lpt1` to `lpt9`), alone or before a dot.
 *
 * Two different labels never name the same folder, also on a file system that ignores case or drops a trailing dot.
 *
 * @param label the text to check.
 * @param subject what the label is, such as "catalog version", used in the error.
 * @throws IllegalArgumentException when the label breaks the rule.
 */
internal fun requireFolderLabel(label: String, subject: String) {
    require(label.isFolderLabel()) {
        "A $subject must be 1 to $MAX_LENGTH characters of lowercase ASCII letters, digits, dots, hyphens and " +
            "underscores, must not start or end with a dot, and must not be a Windows device name, " +
            "alone or before a dot, but was '$label'"
    }
}

private fun String.isFolderLabel(): Boolean =
    length in 1..MAX_LENGTH &&
        all { it.isAllowed() } &&
        !startsWith(DOT) &&
        !endsWith(DOT) &&
        substringBefore(DOT) !in DEVICE_NAMES

private fun Char.isAllowed(): Boolean = this in 'a'..'z' || this in '0'..'9' || this in ALLOWED_PUNCTUATION
