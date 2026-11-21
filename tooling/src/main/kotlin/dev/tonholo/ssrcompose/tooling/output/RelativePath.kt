package dev.tonholo.ssrcompose.tooling.output

private const val SEPARATOR = '/'
private val FORBIDDEN_SEGMENTS = setOf("", ".", "..")
private val FORBIDDEN_CHARACTERS = setOf('\\', ':')

/**
 * A path inside an output folder, with `/` between segments. It is never absolute and never leaves its folder,
 * so an output written by it holds no machine's path.
 *
 * @throws IllegalArgumentException when [value] is empty, starts with `/`, has an empty, `.` or `..` segment, or
 * holds a backslash or a colon.
 */
@JvmInline
public value class RelativePath(public val value: String) {
    init {
        require(value.none { it in FORBIDDEN_CHARACTERS }) {
            "An output path must not hold a backslash or a colon: $value"
        }
        require(value.split(SEPARATOR).none { it in FORBIDDEN_SEGMENTS }) {
            "An output path must be relative and stay inside its folder: $value"
        }
    }
}
