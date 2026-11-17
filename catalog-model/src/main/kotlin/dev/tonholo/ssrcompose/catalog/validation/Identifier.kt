package dev.tonholo.ssrcompose.catalog.validation

private val HARD_KEYWORDS = setOf(
    "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in", "interface", "is",
    "null", "object", "package", "return", "super", "this", "throw", "true", "try", "typealias", "typeof",
    "val", "var", "when", "while",
)

private const val UNDERSCORE = '_'

private fun Char.startsIdentifier(): Boolean = isLetter() || this == UNDERSCORE

private fun Char.continuesIdentifier(): Boolean = isLetterOrDigit() || this == UNDERSCORE

private fun String.isOnlyUnderscores(): Boolean = all { it == UNDERSCORE }

/**
 * True when [text] is a non-empty plain Kotlin identifier of at most [CatalogLimits.MAX_NAME_LENGTH] characters,
 * not made only of underscores, and not a hard keyword.
 */
public fun isKotlinIdentifier(text: String): Boolean =
    text.isNotEmpty() &&
        text.length <= CatalogLimits.MAX_NAME_LENGTH &&
        text.first().startsIdentifier() &&
        text.all { it.continuesIdentifier() } &&
        !text.isOnlyUnderscores() &&
        text !in HARD_KEYWORDS

internal fun requireIdentifier(text: String, what: String) {
    require(isKotlinIdentifier(text = text)) {
        "$what must be a Kotlin identifier of at most ${CatalogLimits.MAX_NAME_LENGTH} characters, " +
            "was \"${text.cutForMessage()}\""
    }
}
