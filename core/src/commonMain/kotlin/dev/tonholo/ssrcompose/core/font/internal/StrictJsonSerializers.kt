package dev.tonholo.ssrcompose.core.font.internal

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

private fun Decoder.decodeUnquotedPrimitive(): JsonPrimitive {
    val primitive = (this as? JsonDecoder)?.decodeJsonElement() as? JsonPrimitive
    if (primitive == null || primitive.isString) {
        throw SerializationException("An unquoted JSON number or boolean was expected")
    }
    return primitive
}

/** Reads an integer written as a JSON number, and refuses one written as a string. */
internal object StrictIntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("StrictInt", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int =
        decoder.decodeUnquotedPrimitive().content.toIntOrNull()
            ?: throw SerializationException("An integer was expected")

    override fun serialize(encoder: Encoder, value: Int) = encoder.encodeInt(value = value)
}

/** Reads a boolean written as a JSON boolean, and refuses one written as a string. */
internal object StrictBooleanSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("StrictBoolean", PrimitiveKind.BOOLEAN)

    override fun deserialize(decoder: Decoder): Boolean =
        decoder.decodeUnquotedPrimitive().content.toBooleanStrictOrNull()
            ?: throw SerializationException("A boolean was expected")

    override fun serialize(encoder: Encoder, value: Boolean) = encoder.encodeBoolean(value = value)
}
