package dev.tonholo.ssrcompose.core.document

import dev.tonholo.ssrcompose.core.annotation.InternalSsrComposeApi
import kotlinx.serialization.Serializable

/**
 * What accessibility services read for a node, from the producer's semantics.
 */
@InternalSsrComposeApi
@Serializable
public data class NodeSemantics(
    val description: String? = null,
    val heading: Boolean = false,
    val role: NodeRole? = null,
)
