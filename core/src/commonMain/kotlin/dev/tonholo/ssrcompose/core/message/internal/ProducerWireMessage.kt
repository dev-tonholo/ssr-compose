package dev.tonholo.ssrcompose.core.message.internal

import dev.tonholo.ssrcompose.core.handshake.Answer
import dev.tonholo.ssrcompose.core.handshake.ContractVersion
import dev.tonholo.ssrcompose.core.handshake.RefusalKind
import dev.tonholo.ssrcompose.core.message.ProducerMessage
import dev.tonholo.ssrcompose.core.message.ScreenContent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface ProducerWireMessage {
    @Serializable
    @SerialName("Accepted")
    data class Accepted(val contractVersion: ContractVersion) : ProducerWireMessage

    @Serializable
    @SerialName("Refused")
    data class Refused(val kind: RefusalKind, val reason: String) : ProducerWireMessage

    @Serializable
    @SerialName("Screen")
    data class Screen(val screen: String, val content: ScreenContent) : ProducerWireMessage

    @Serializable
    @SerialName("ScreenUnavailable")
    data class ScreenUnavailable(val screen: String, val reason: String) : ProducerWireMessage
}

internal fun ProducerMessage.toWire(): ProducerWireMessage = when (this) {
    is ProducerMessage.Answered -> answer.toWire()
    is ProducerMessage.Screen -> ProducerWireMessage.Screen(screen = screen, content = content)
    is ProducerMessage.ScreenUnavailable ->
        ProducerWireMessage.ScreenUnavailable(screen = screen, reason = reason)
}

private fun Answer.toWire(): ProducerWireMessage = when (this) {
    is Answer.Accepted -> ProducerWireMessage.Accepted(contractVersion = contractVersion)
    is Answer.Refused -> ProducerWireMessage.Refused(kind = kind, reason = reason)
}

internal fun ProducerWireMessage.toMessage(): ProducerMessage = when (this) {
    is ProducerWireMessage.Accepted ->
        ProducerMessage.Answered(answer = Answer.Accepted(contractVersion = contractVersion))
    is ProducerWireMessage.Refused ->
        ProducerMessage.Answered(answer = Answer.Refused(kind = kind, reason = reason))
    is ProducerWireMessage.Screen -> ProducerMessage.Screen(screen = screen, content = content)
    is ProducerWireMessage.ScreenUnavailable ->
        ProducerMessage.ScreenUnavailable(screen = screen, reason = reason)
}
