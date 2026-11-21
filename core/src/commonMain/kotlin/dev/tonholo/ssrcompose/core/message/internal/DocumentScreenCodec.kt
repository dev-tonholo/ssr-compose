package dev.tonholo.ssrcompose.core.message.internal

import kotlinx.serialization.json.Json

/**
 * Reads and writes the `Screen` message that carries a document, with the same frame rules as [JsonMessageCodec].
 */
internal object DocumentScreenCodec {
    private val json = Json

    fun encode(screen: DocumentScreen): ByteArray =
        json.encodeToString(serializer = DocumentWireMessage.serializer(), value = screen).encodeToByteArray()

    fun decode(frame: ByteArray): DocumentScreen =
        when (val message = json.decodeFrame(strategy = DocumentWireMessage.serializer(), payload = frame)) {
            is DocumentScreen -> message
        }
}
