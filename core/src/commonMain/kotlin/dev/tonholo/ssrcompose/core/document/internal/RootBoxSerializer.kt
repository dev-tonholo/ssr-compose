package dev.tonholo.ssrcompose.core.document.internal

import dev.tonholo.ssrcompose.core.document.DocumentNode
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Writes the root with its node type, like every other node, and reads only a `Box` there. */
internal object RootBoxSerializer : KSerializer<DocumentNode.Box> {
    private val nodeSerializer = DocumentNode.serializer()

    override val descriptor: SerialDescriptor = nodeSerializer.descriptor

    override fun serialize(encoder: Encoder, value: DocumentNode.Box) {
        encoder.encodeSerializableValue(serializer = nodeSerializer, value = value)
    }

    override fun deserialize(decoder: Decoder): DocumentNode.Box =
        decoder.decodeSerializableValue(deserializer = nodeSerializer) as? DocumentNode.Box
            ?: throw SerializationException(message = "The root of a document must be a Box")
}
