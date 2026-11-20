package dev.tonholo.ssrcompose.core.document

import kotlinx.serialization.Serializable

/**
 * What accessibility services read for a node, from the producer's semantics.
 */
@Serializable
public data class NodeSemantics(
    val description: String? = null,
    val heading: Boolean = false,
    val role: NodeRole? = null,
)
