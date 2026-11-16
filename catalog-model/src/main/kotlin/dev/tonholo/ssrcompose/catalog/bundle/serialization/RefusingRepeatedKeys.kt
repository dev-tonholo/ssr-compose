package dev.tonholo.ssrcompose.catalog.bundle.serialization

import dev.tonholo.ssrcompose.catalog.bundle.CatalogFileException

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder

/**
 * Reads like [delegate] and throws a [CatalogFileException] when an object has the same key twice, at any depth.
 */
internal class RefusingRepeatedKeys<T>(private val delegate: KSerializer<T>) : KSerializer<T> by delegate {
    override fun deserialize(decoder: Decoder): T = delegate.deserialize(RepeatedKeyDecoder(inner = decoder))
}

private class RepeatedKeyDecoder(private val inner: Decoder) : Decoder by inner {
    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder =
        RepeatedKeyStructure(inner = inner.beginStructure(descriptor = descriptor), descriptor = descriptor)
}

@OptIn(ExperimentalSerializationApi::class)
private class RepeatedKeyStructure(
    private val inner: CompositeDecoder,
    descriptor: SerialDescriptor,
) : CompositeDecoder by inner {
    private val seen: BooleanArray? =
        if (descriptor.kind == StructureKind.CLASS) BooleanArray(size = descriptor.elementsCount) else null

    override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
        val index = inner.decodeElementIndex(descriptor = descriptor)
        if (seen != null && index >= 0) {
            if (seen[index]) throw CatalogFileException(message = "a key is repeated")
            seen[index] = true
        }
        return index
    }

    override fun <T> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        previousValue: T?,
    ): T = inner.decodeSerializableElement(
        descriptor = descriptor,
        index = index,
        deserializer = deserializer.refusingRepeatedKeys(),
        previousValue = previousValue,
    )

    override fun <T : Any> decodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>,
        previousValue: T?,
    ): T? = inner.decodeNullableSerializableElement(
        descriptor = descriptor,
        index = index,
        deserializer = deserializer.refusingRepeatedKeys(),
        previousValue = previousValue,
    )
}

private fun <T> DeserializationStrategy<T>.refusingRepeatedKeys(): DeserializationStrategy<T> =
    if (this is KSerializer<T> && descriptor.kind is StructureKind) RefusingRepeatedKeys(delegate = this) else this
