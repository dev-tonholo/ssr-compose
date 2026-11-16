package dev.tonholo.ssrcompose.catalog.bundle

import kotlinx.serialization.SerializationException
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.json.JsonDecoder

internal fun Decoder.asJsonDecoder(): JsonDecoder =
    this as? JsonDecoder ?: throw SerializationException(message = "The catalog file is read as JSON only")
