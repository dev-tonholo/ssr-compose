package dev.tonholo.ssrcompose.core.document.internal

import dev.tonholo.ssrcompose.core.document.DocumentColor
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

private const val PREFIX = '#'
private const val HEX_DIGITS = 8
private const val HEX_RADIX = 16

internal object DocumentColorSerializer : KSerializer<DocumentColor> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(serialName = "DocumentColor", kind = PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: DocumentColor) {
        val digits = value.argb.toString(radix = HEX_RADIX).uppercase().padStart(length = HEX_DIGITS, padChar = '0')
        encoder.encodeString(value = PREFIX + digits)
    }

    override fun deserialize(decoder: Decoder): DocumentColor {
        val text = decoder.decodeString()
        val digits = text.removePrefix(prefix = PREFIX.toString())
        if (digits.length != HEX_DIGITS || digits.length == text.length || !digits.all(predicate = ::isHexDigit)) {
            throw SerializationException(message = "A color must be written #AARRGGBB")
        }
        return DocumentColor(argb = digits.toUInt(radix = HEX_RADIX))
    }

    private fun isHexDigit(char: Char): Boolean = char in '0'..'9' || char.lowercaseChar() in 'a'..'f'
}
