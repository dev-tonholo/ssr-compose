package dev.tonholo.ssrcompose.core.handshake

import kotlinx.serialization.Serializable

/**
 * Fixed for a session. Self-declared and not proven.
 */
@Serializable
public data class AppIdentity(
    val version: String,
    val variant: String,
    val flavor: String = "",
)
