package dev.tonholo.ssrcompose.catalog.validation

private const val BYTES_PER_MEBIBYTE = 1024 * 1024
private const val DEFAULT_FILE_MEBIBYTES = 16

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
}
