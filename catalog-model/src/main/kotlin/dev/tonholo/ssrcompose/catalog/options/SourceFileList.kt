package dev.tonholo.ssrcompose.catalog.options

/** The text of a file that lists source paths. The build writes it and the compiler plugin reads it. */
public object SourceFileList {
    private const val SEPARATOR = '\u0000'

    /** Writes [paths] as one text. No file path can hold the separator. */
    public fun encode(paths: Collection<String>): String = paths.joinToString(separator = SEPARATOR.toString())

    /** Reads the paths written by [encode]. */
    public fun decode(text: String): List<String> = text.split(SEPARATOR).filter { path -> path.isNotEmpty() }
}
