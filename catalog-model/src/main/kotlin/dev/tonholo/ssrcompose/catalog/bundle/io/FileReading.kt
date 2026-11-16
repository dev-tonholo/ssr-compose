package dev.tonholo.ssrcompose.catalog.bundle.io

import dev.tonholo.ssrcompose.catalog.bundle.CatalogFileException
import dev.tonholo.ssrcompose.catalog.bundle.SizeCap
import dev.tonholo.ssrcompose.catalog.bundle.serialization.CatalogFormat
import dev.tonholo.ssrcompose.catalog.bundle.serialization.FormatHeader
import dev.tonholo.ssrcompose.catalog.bundle.serialization.RefusingRepeatedKeys
import dev.tonholo.ssrcompose.catalog.bundle.serialization.catalogJson
import dev.tonholo.ssrcompose.catalog.bundle.serialization.describe
import dev.tonholo.ssrcompose.catalog.bundle.serialization.shapeJson
import java.io.OutputStream
import kotlinx.serialization.KSerializer

/**
 * Reads one file of a catalog and gives its model.
 *
 * @param subject what the file is, such as `part app`. Every failure message starts with it.
 * @throws CatalogFileException when the file is over [sizeCap], is not valid UTF-8, is not a valid file of the
 * known format, [toModel] rejects what it holds, or the bytes differ from the writer's canonical form.
 */
internal fun <F, M> readFile(
    subject: String,
    bytes: ByteArray,
    sizeCap: SizeCap,
    serializer: KSerializer<F>,
    toModel: (F) -> M,
    writeModel: (M, OutputStream) -> Unit,
): M {
    val text = bytes.requireWithin(sizeCap = sizeCap, subject = subject).decodeUtf8(subject = subject)
    val strictKeys = RefusingRepeatedKeys(delegate = serializer)
    return try {
        requireKnownFormat(text = text)
        shapeJson.decodeFromString(deserializer = strictKeys, string = text)
        val model = toModel(catalogJson.decodeFromString(deserializer = strictKeys, string = text))
        requireCanonicalBytes(expected = bytes, model = model, writeModel = writeModel)
        model
    } catch (exception: IllegalArgumentException) {
        throw notValid(subject = subject, detail = describe(exception = exception))
    } catch (exception: CatalogFileException) {
        throw notValid(subject = subject, detail = describe(exception = exception))
    }
}

private fun <M> requireCanonicalBytes(expected: ByteArray, model: M, writeModel: (M, OutputStream) -> Unit) {
    val comparison = ByteComparisonOutputStream(expected = expected)
    writeModel(model, comparison)
    if (!comparison.matches) {
        throw CatalogFileException(message = "it is not in canonical form")
    }
}

private class ByteComparisonOutputStream(private val expected: ByteArray) : OutputStream() {
    private var position: Int = 0
    private var mismatch: Boolean = false

    val matches: Boolean
        get() = !mismatch && position == expected.size

    override fun write(byte: Int) {
        if (mismatch || position >= expected.size || expected[position] != byte.toByte()) {
            mismatch = true
            return
        }
        position++
    }

    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        if (mismatch || length > expected.size - position) {
            mismatch = true
            return
        }
        for (i in 0 until length) {
            if (expected[position + i] != bytes[offset + i]) {
                mismatch = true
                return
            }
        }
        position += length
    }
}

private fun notValid(subject: String, detail: String): CatalogFileException =
    CatalogFileException(message = "The $subject is not valid: $detail")

private fun requireKnownFormat(text: String) {
    val header = shapeJson.decodeFromString(deserializer = FormatHeader.serializer(), string = text)
    if (header.format != CatalogFormat.CURRENT) {
        throw CatalogFileException(message = "its format ${header.format} is not known")
    }
}
