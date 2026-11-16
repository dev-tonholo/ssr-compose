package dev.tonholo.ssrcompose.catalog.validation

private const val BYTES_PER_MEBIBYTE = 1024 * 1024
private const val DEFAULT_FILE_MEBIBYTES = 16
private const val DEEPEST_LEGAL_NESTING = 8
private const val NESTING_MARGIN = 2

/**
 * The bounds a catalog must stay inside. They keep a file small enough to read and review.
 */
internal object CatalogLimits {
    const val MAX_NAME_LENGTH: Int = 64
    const val MAX_CALLBACK_ARGUMENTS: Int = 4

    /**
     * The longest catalog text a reader accepts by default, 16 MiB. It also bounds the number of choice lists,
     * choice entries and slot arguments. The number is provisional.
     */
    const val MAX_FILE_LENGTH: Int = DEFAULT_FILE_MEBIBYTES * BYTES_PER_MEBIBYTE

    /**
     * The deepest nesting of arrays and objects a catalog file may have. The deepest legal file nests 8 levels,
     * for a slot argument that is a list of a choice, and the rest is margin.
     */
    const val MAX_NESTING_DEPTH: Int = DEEPEST_LEGAL_NESTING + NESTING_MARGIN
}
