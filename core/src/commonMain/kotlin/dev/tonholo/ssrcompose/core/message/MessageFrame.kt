package dev.tonholo.ssrcompose.core.message

import dev.tonholo.ssrcompose.core.annotation.InternalSsrComposeApi
import dev.tonholo.ssrcompose.core.handshake.ContractVersion

/**
 * The layout of the consumer's first frame, the one that carries `Introduce`.
 * It starts with the contract version as a big-endian number of [CONTRACT_VERSION_BYTES] bytes, then the codec's
 * payload. Every codec can read the version without decoding the payload. Later frames are the payload alone.
 */
@InternalSsrComposeApi
public object MessageFrame {
    public const val CONTRACT_VERSION_BYTES: Int = Int.SIZE_BYTES

    private const val BITS_PER_BYTE = 8
    private const val BYTE_MASK = 0xFF

    /**
     * Reads the contract version of a first frame.
     * @throws MessageDecodingException when the frame is shorter than [CONTRACT_VERSION_BYTES].
     */
    public fun readContractVersion(frame: ByteArray): ContractVersion {
        requirePrefix(frame = frame)
        val value = frame.take(n = CONTRACT_VERSION_BYTES)
            .fold(initial = 0) { accumulated, byte -> (accumulated shl BITS_PER_BYTE) or (byte.toInt() and BYTE_MASK) }
        return ContractVersion(value = value)
    }

    internal fun withContractVersion(version: ContractVersion, payload: ByteArray): ByteArray {
        val prefix = ByteArray(size = CONTRACT_VERSION_BYTES) { index ->
            val shift = (CONTRACT_VERSION_BYTES - 1 - index) * BITS_PER_BYTE
            (version.value shr shift).toByte()
        }
        return prefix + payload
    }

    internal fun payloadOf(firstFrame: ByteArray): ByteArray {
        requirePrefix(frame = firstFrame)
        return firstFrame.copyOfRange(fromIndex = CONTRACT_VERSION_BYTES, toIndex = firstFrame.size)
    }

    private fun requirePrefix(frame: ByteArray) {
        if (frame.size < CONTRACT_VERSION_BYTES) {
            throw MessageDecodingException(message = "The first frame is too short to hold a contract version")
        }
    }
}
