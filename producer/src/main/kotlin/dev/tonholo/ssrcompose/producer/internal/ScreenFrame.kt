package dev.tonholo.ssrcompose.producer.internal

import dev.tonholo.ssrcompose.core.document.ScreenDocument
import dev.tonholo.ssrcompose.producer.internal.reader.NotSendableReason

/**
 * What a live screen has to send after a change: a document with its revision, or why it cannot be sent.
 */
internal sealed interface ScreenFrame {
    data class Sent(val revision: Long, val document: ScreenDocument) : ScreenFrame

    data class NotSendable(val reason: NotSendableReason) : ScreenFrame
}
