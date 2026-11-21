package dev.tonholo.ssrcompose.core.identifier

/** The most characters a name the library checks as a Kotlin identifier may have. */
public const val MAX_IDENTIFIER_LENGTH: Int = 64

private val HARD_KEYWORDS = setOf(
    "as", "break", "class", "continue", "do", "else", "false", "for", "fun", "if", "in", "interface", "is",
    "null", "object", "package", "return", "super", "this", "throw", "true", "try", "typealias", "typeof",
    "val", "var", "when", "while",
)

private const val UNDERSCORE = '_'

private fun Char.startsIdentifier(): Boolean = isLetter() || this == UNDERSCORE

private fun Char.continuesIdentifier(): Boolean = isLetterOrDigit() || this == UNDERSCORE

private fun String.isOnlyUnderscores(): Boolean = all { it == UNDERSCORE }

private fun hasIdentifierShape(text: String): Boolean =
    text.isNotEmpty() &&
        text.first().startsIdentifier() &&
        text.all { it.continuesIdentifier() } &&
        !text.isOnlyUnderscores()

/**
 * True when [text] is a non-empty plain Kotlin identifier of at most [MAX_IDENTIFIER_LENGTH] characters, not made
 * only of underscores, and not a hard keyword.
 */
public fun isKotlinIdentifier(text: String): Boolean =
    text.length <= MAX_IDENTIFIER_LENGTH && hasIdentifierShape(text = text) && text !in HARD_KEYWORDS

/**
 * True when [text] is segments of any length separated by single dots, each with the shape of a Kotlin identifier.
 * A segment may be a hard keyword, as in `in.acme.bridge`, because source code can name it with backticks.
 */
public fun isKotlinPackageName(text: String): Boolean =
    text.split('.').all { segment -> hasIdentifierShape(text = segment) }
