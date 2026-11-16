package dev.tonholo.ssrcompose.catalog.bundle

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/**
 * A whole number written with digits only. The default decoder also accepts a quoted number.
 * It reads the value as a tree, so it must only run on a value already known to be a primitive.
 */
internal object WholeNumberSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "CatalogWholeNumber",
        kind = PrimitiveKind.INT,
    )

    override fun deserialize(decoder: Decoder): Int {
        val content = decoder.unquotedPrimitive()?.content
        return content?.toIntOrNull()?.takeIf { it.toString() == content }
            ?: throw CatalogFileException(message = "A whole number must be written with digits only, without quotes")
    }

    override fun serialize(encoder: Encoder, value: Int) {
        encoder.encodeInt(value = value)
    }
}

/**
 * A flag written as `true` or `false` without quotes. The default decoder also accepts a quoted flag.
 * It reads the value as a tree, so it must only run on a value already known to be a primitive.
 */
internal object FlagSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        serialName = "CatalogFlag",
        kind = PrimitiveKind.BOOLEAN,
    )

    override fun deserialize(decoder: Decoder): Boolean =
        decoder.unquotedPrimitive()?.booleanOrNull
            ?: throw CatalogFileException(message = "A flag must be true or false, written without quotes")

    override fun serialize(encoder: Encoder, value: Boolean) {
        encoder.encodeBoolean(value = value)
    }
}

private fun Decoder.unquotedPrimitive(): JsonPrimitive? =
    (asJsonDecoder().decodeJsonElement() as? JsonPrimitive)?.takeUnless { it.isString }
