package dev.tonholo.ssrcompose.core.message

import dev.tonholo.ssrcompose.core.handshake.ContractVersion
import dev.tonholo.ssrcompose.core.message.internal.JsonMessageCodec

/**
 * Turns messages into frames and back, for one contract version.
 * `Introduce` is only valid as the consumer's first frame, which has its own pair of functions and starts with the
 * contract version, see [MessageFrame]. Every other frame is the payload alone.
 * Decoding throws [MessageDecodingException] and nothing else.
 */
public interface MessageCodec {
    public val contractVersion: ContractVersion

    /** Encodes the consumer's first frame: the contract version, then the `Introduce`. */
    public fun encodeFirstFrame(introduce: ConsumerMessage.Introduce): ByteArray

    /**
     * Decodes the consumer's first frame. The prefix, the codec and the contract version inside the `Introduce`
     * must all agree.
     */
    public fun decodeFirstFrame(frame: ByteArray): ConsumerMessage.Introduce

    /** Encodes a frame without a prefix. */
    public fun encode(message: ConsumerMessage): ByteArray

    public fun encode(message: ProducerMessage): ByteArray

    /** Decodes a frame that is not the first one. It rejects `Introduce`. */
    public fun decodeConsumerMessage(frame: ByteArray): ConsumerMessage

    public fun decodeProducerMessage(frame: ByteArray): ProducerMessage

    public companion object {
        /** The JSON codec for contract version 1. */
        public fun json(): MessageCodec = JsonMessageCodec()
    }
}
