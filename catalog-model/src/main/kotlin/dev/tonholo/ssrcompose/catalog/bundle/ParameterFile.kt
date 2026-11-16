package dev.tonholo.ssrcompose.catalog.bundle

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

/**
 * A parameter as it is written in a file. The `kind` says which of the other fields belong to it: [choice] and [of]
 * to a value, [arguments] to a callback or a slot, [accepts] to a slot, [nullable] to a value.
 */
@Serializable
internal data class ParameterFile(
    val name: String,
    val kind: String,
    val choice: String? = null,
    val of: ElementKindFile? = null,
    val arguments: List<KindFile>? = null,
    @Contextual val nullable: Boolean = false,
    val accepts: String? = null,
    @Contextual val required: Boolean,
)
