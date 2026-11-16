package dev.tonholo.ssrcompose.core.message

import dev.tonholo.ssrcompose.core.data.DataValue
import dev.tonholo.ssrcompose.core.handshake.DeviceContext
import dev.tonholo.ssrcompose.core.handshake.Introduction
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Messages from the consumer to the producer.
 */
@Serializable
public sealed interface ConsumerMessage {
    /** The first message, once per session. */
    @Serializable
    @SerialName("Introduce")
    public data class Introduce(val introduction: Introduction) : ConsumerMessage

    /** Asks for a screen by name. Asking again, or for another name, replaces the open screen. */
    @Serializable
    @SerialName("OpenScreen")
    public data class OpenScreen(val screen: String) : ConsumerMessage

    @Serializable
    @SerialName("DeviceChanged")
    public data class DeviceChanged(val device: DeviceContext) : ConsumerMessage

    /** The user triggered a callback of a placed component. [arguments] is empty when the callback has none. */
    @Serializable
    @SerialName("Trigger")
    public data class Trigger(val action: ActionId, val arguments: List<DataValue>) : ConsumerMessage
}
