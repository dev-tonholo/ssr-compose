package dev.tonholo.ssrcompose.catalog.validation

/**
 * The bounds a catalog must stay inside. They keep a file small enough to read and review.
 */
internal object CatalogLimits {
    const val MAX_NAME_LENGTH: Int = 64
    const val MAX_CALLBACK_ARGUMENTS: Int = 4
    const val MAX_FRAGMENT_FILE_NAME_BYTES: Int = 255
}
