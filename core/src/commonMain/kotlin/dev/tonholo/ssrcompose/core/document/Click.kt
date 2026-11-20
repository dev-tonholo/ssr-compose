package dev.tonholo.ssrcompose.core.document

import dev.tonholo.ssrcompose.core.annotation.InternalSsrComposeApi
import dev.tonholo.ssrcompose.core.message.ActionId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * What a tap on a node does.
 */
@InternalSsrComposeApi
@Serializable
public sealed interface Click {
    /** Sends a `Trigger` for [action] with no arguments. */
    @Serializable
    @SerialName("Remote")
    public data class Remote(
        val action: ActionId,
        val label: String? = null,
        val role: NodeRole? = null,
    ) : Click

    /** Shows [variant] of the switcher whose node id is [switcher] at once, then sends `Switched`. */
    @Serializable
    @SerialName("Local")
    public data class Local(val switcher: Int, val variant: Int) : Click
}
