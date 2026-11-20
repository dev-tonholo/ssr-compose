package dev.tonholo.ssrcompose.core.message

import dev.tonholo.ssrcompose.core.annotation.InternalSsrComposeApi
import dev.tonholo.ssrcompose.core.data.DataValue
import dev.tonholo.ssrcompose.core.document.Measurement
import dev.tonholo.ssrcompose.core.document.Viewport
import dev.tonholo.ssrcompose.core.font.FontBundleVersion
import dev.tonholo.ssrcompose.core.handshake.DeviceContext
import dev.tonholo.ssrcompose.core.handshake.Introduction
import dev.tonholo.ssrcompose.core.message.internal.requireMeasurements
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Messages from the consumer to the producer.
 */
@Serializable
@InternalSsrComposeApi
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

    /** The space the app gives the open screen changed. */
    @Serializable
    @SerialName("ViewportChanged")
    public data class ViewportChanged(val viewport: Viewport) : ConsumerMessage

    /**
     * The sizes the device measured for the app components of the document of [revision].
     *
     * @throws IllegalArgumentException when it holds more than 10,000 measurements or two for one component.
     */
    @Serializable
    @SerialName("ComponentMeasured")
    public data class ComponentMeasured(val revision: Long, val components: List<Measurement>) : ConsumerMessage {
        init {
            requireMeasurements(components = components)
        }
    }

    /** The app's fonts changed to [fontVersion] during the session; null when the app now declares no font. */
    @Serializable
    @SerialName("FontVersionChanged")
    public data class FontVersionChanged(val fontVersion: FontBundleVersion? = null) : ConsumerMessage

    /** A local click already shows [variant] of the switcher whose node id is [switcher]. */
    @Serializable
    @SerialName("Switched")
    public data class Switched(val switcher: Int, val variant: Int) : ConsumerMessage
}
