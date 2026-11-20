package dev.tonholo.ssrcompose.producer.internal.reader

import dev.tonholo.ssrcompose.core.document.ScreenDocument

/**
 * What the reader made of a screen: the positioned document, or why it cannot be sent.
 */
internal sealed interface ScreenRead {
    data class Document(val document: ScreenDocument) : ScreenRead

    data class NotSendable(val reason: NotSendableReason) : ScreenRead
}
