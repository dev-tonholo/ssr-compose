package dev.tonholo.ssrcompose.core.message.internal

import dev.tonholo.ssrcompose.core.message.MessageDecodingException
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json

/**
 * Reads a UTF-8 JSON frame with [strategy]. Nesting deeper than [MAX_NESTING_DEPTH] is refused, and every failure is
 * a [MessageDecodingException].
 */
internal fun <T> Json.decodeFrame(strategy: DeserializationStrategy<T>, payload: ByteArray): T =
    try {
        val element = parseToJsonElement(string = payload.decodeToString())
        if (element.exceedsNestingDepth(limit = MAX_NESTING_DEPTH)) {
            throw MessageDecodingException(message = "The frame is nested too deeply")
        }
        decodeFromJsonElement(deserializer = strategy, element = element)
    } catch (cause: IllegalArgumentException) {
        throw MessageDecodingException(message = "The frame is not a message", cause = cause)
    }
