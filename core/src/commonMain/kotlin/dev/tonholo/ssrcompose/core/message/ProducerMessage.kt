package dev.tonholo.ssrcompose.core.message

import dev.tonholo.ssrcompose.core.annotation.InternalSsrComposeApi
import dev.tonholo.ssrcompose.core.handshake.Answer

/**
 * Messages from the producer to the consumer.
 * On the wire an [Answered] shows as `Accepted` or `Refused`, at the same level as the other messages.
 */
@InternalSsrComposeApi
public sealed interface ProducerMessage {
    public data class Answered(val answer: Answer) : ProducerMessage

    public data class Screen(val screen: String, val content: ScreenContent) : ProducerMessage

    public data class ScreenUnavailable(val screen: String, val reason: String) : ProducerMessage
}
