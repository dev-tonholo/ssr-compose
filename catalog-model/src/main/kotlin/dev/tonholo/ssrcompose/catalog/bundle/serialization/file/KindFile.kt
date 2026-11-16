package dev.tonholo.ssrcompose.catalog.bundle.serialization.file

import kotlinx.serialization.Serializable

/**
 * A value kind as it is written in a file: always an object with a `kind`, and a `choice` or an `of` when the kind
 * needs one.
 */
@Serializable
internal data class KindFile(
    val kind: String,
    val choice: String? = null,
    val of: ElementKindFile? = null,
)

/**
 * The element of a list kind. It has no `of`, so a list of lists cannot be written in a file.
 */
@Serializable
internal data class ElementKindFile(val kind: String, val choice: String? = null)
