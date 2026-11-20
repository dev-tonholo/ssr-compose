package dev.tonholo.ssrcompose.catalog.validation

import dev.tonholo.ssrcompose.core.identifier.MAX_IDENTIFIER_LENGTH

/**
 * The bounds a catalog must stay inside. They keep a file small enough to read and review.
 */
public object CatalogLimits {
    /** Maximum length in characters for a component, parameter, or choice list identifier. */
    public const val MAX_NAME_LENGTH: Int = MAX_IDENTIFIER_LENGTH

    internal const val MAX_CALLBACK_ARGUMENTS: Int = 4
    internal const val MAX_FRAGMENT_FILE_NAME_BYTES: Int = 255
}
