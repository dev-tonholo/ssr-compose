package dev.tonholo.ssrcompose.core.message

/**
 * A frame could not be read as a message. It is the only error a codec throws when decoding.
 */
public class MessageDecodingException(message: String, cause: Throwable? = null) : Exception(message, cause)
