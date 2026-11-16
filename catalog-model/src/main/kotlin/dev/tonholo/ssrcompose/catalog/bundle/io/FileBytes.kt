package dev.tonholo.ssrcompose.catalog.bundle.io

import dev.tonholo.ssrcompose.catalog.bundle.CatalogFileException
import dev.tonholo.ssrcompose.catalog.bundle.SizeCap
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.CodingErrorAction

internal fun ByteArray.requireWithin(sizeCap: SizeCap, subject: String): ByteArray {
    if (size > sizeCap.bytes) {
        throw CatalogFileException(message = "The $subject is larger than ${sizeCap.bytes} bytes")
    }
    return this
}

internal fun ByteArray.decodeUtf8(subject: String): String = try {
    Charsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT)
        .decode(ByteBuffer.wrap(this))
        .toString()
} catch (ignored: CharacterCodingException) {
    throw CatalogFileException(message = "The $subject is not valid UTF-8")
}
