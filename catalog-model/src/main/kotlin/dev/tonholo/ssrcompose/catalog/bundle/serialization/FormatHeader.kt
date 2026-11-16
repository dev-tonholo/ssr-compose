package dev.tonholo.ssrcompose.catalog.bundle.serialization

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

internal object CatalogFormat {
    const val CURRENT: Int = 1
}

/**
 * Only the format of a file, so it can be read before the rest.
 */
@JsonIgnoreUnknownKeys
@Serializable
internal data class FormatHeader(@Contextual val format: Int)
