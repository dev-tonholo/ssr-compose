package dev.tonholo.ssrcompose.core.message.internal

import dev.tonholo.ssrcompose.core.document.ScreenDocument
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The `Screen` message that carries a [ScreenDocument], read and written beside the older `Screen` until it
 * replaces it.
 */
@Serializable
internal sealed interface DocumentWireMessage

@Serializable
@SerialName("Screen")
internal data class DocumentScreen(
    val screen: String,
    val revision: Long,
    val pending: Boolean,
    val content: ScreenDocument,
) : DocumentWireMessage
