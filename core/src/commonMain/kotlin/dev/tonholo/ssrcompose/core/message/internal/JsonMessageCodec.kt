package dev.tonholo.ssrcompose.core.message.internal

import dev.tonholo.ssrcompose.core.handshake.ContractVersion
import dev.tonholo.ssrcompose.core.message.ConsumerMessage
import dev.tonholo.ssrcompose.core.message.MessageCodec
import dev.tonholo.ssrcompose.core.message.MessageDecodingException
import dev.tonholo.ssrcompose.core.message.MessageFrame
import dev.tonholo.ssrcompose.core.message.ProducerMessage
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json

/**
 * The JSON codec for contract version 1. A frame is UTF-8 JSON, except the first consumer frame, which has
 * the contract version in front of the JSON, see [MessageFrame]. Nesting deeper than [MAX_NESTING_DEPTH] is refused.
 */
internal class JsonMessageCodec : MessageCodec {
    private val json = Json

    override val contractVersion: ContractVersion = ContractVersion(value = 1)

    override fun encodeFirstFrame(introduce: ConsumerMessage.Introduce): ByteArray =
        MessageFrame.withContractVersion(version = contractVersion, payload = encode(message = introduce))

    override fun decodeFirstFrame(frame: ByteArray): ConsumerMessage.Introduce {
        val prefixVersion = requireContractVersion(version = MessageFrame.readContractVersion(frame = frame))
        val payload = MessageFrame.payloadOf(firstFrame = frame)
        val message = decode(strategy = ConsumerMessage.serializer(), payload = payload)
        return requireSameContractVersion(introduce = asIntroduce(message = message), prefixVersion = prefixVersion)
    }

    private fun requireContractVersion(version: ContractVersion): ContractVersion {
        if (version != contractVersion) {
            throw MessageDecodingException(message = "This codec reads contract version ${contractVersion.value}")
        }
        return version
    }

    private fun asIntroduce(message: ConsumerMessage): ConsumerMessage.Introduce =
        message as? ConsumerMessage.Introduce
            ?: throw MessageDecodingException(message = "The first frame must be an Introduce")

    private fun requireSameContractVersion(
        introduce: ConsumerMessage.Introduce,
        prefixVersion: ContractVersion,
    ): ConsumerMessage.Introduce {
        if (introduce.introduction.contractVersion != prefixVersion) {
            throw MessageDecodingException(message = "The contract version differs inside the first frame")
        }
        return introduce
    }

    override fun encode(message: ConsumerMessage): ByteArray =
        json.encodeToString(serializer = ConsumerMessage.serializer(), value = message).encodeToByteArray()

    override fun encode(message: ProducerMessage): ByteArray =
        json.encodeToString(serializer = ProducerWireMessage.serializer(), value = message.toWire())
            .encodeToByteArray()

    override fun decodeConsumerMessage(frame: ByteArray): ConsumerMessage {
        val message = decode(strategy = ConsumerMessage.serializer(), payload = frame)
        if (message is ConsumerMessage.Introduce) {
            throw MessageDecodingException(message = "An Introduce is only valid as the first frame")
        }
        return message
    }

    override fun decodeProducerMessage(frame: ByteArray): ProducerMessage =
        decode(strategy = ProducerWireMessage.serializer(), payload = frame).toMessage()

    private fun <T> decode(strategy: DeserializationStrategy<T>, payload: ByteArray): T =
        try {
            val element = json.parseToJsonElement(string = payload.decodeToString())
            if (element.exceedsNestingDepth(limit = MAX_NESTING_DEPTH)) {
                throw MessageDecodingException(message = "The frame is nested too deeply")
            }
            json.decodeFromJsonElement(deserializer = strategy, element = element)
        } catch (cause: IllegalArgumentException) {
            throw MessageDecodingException(message = "The frame is not a message", cause = cause)
        }
}
