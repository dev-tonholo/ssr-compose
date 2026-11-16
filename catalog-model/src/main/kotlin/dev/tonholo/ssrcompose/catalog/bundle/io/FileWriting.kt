package dev.tonholo.ssrcompose.catalog.bundle.io

import dev.tonholo.ssrcompose.catalog.bundle.SizeCap
import dev.tonholo.ssrcompose.catalog.bundle.serialization.catalogJson
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.encodeToStream

private const val TRAILING_NEWLINE = '\n'.code

/**
 * Writes one file into [output]: the JSON text of [file] as UTF-8, then one trailing newline.
 */
@OptIn(ExperimentalSerializationApi::class)
internal fun <F> writeFileTo(serializer: KSerializer<F>, file: F, output: OutputStream) {
    catalogJson.encodeToStream(serializer = serializer, value = file, stream = output)
    output.write(TRAILING_NEWLINE)
}

/**
 * The bytes of one file: the JSON text of [file] as UTF-8, then one trailing newline.
 */
internal fun <F> writeFile(
    serializer: KSerializer<F>,
    file: F,
    subject: String? = null,
    sizeCap: SizeCap? = null,
): ByteArray {
    val buffer = ByteArrayOutputStream()
    val output: OutputStream = if (sizeCap != null && subject != null) {
        SizeCappedOutputStream(delegate = buffer, sizeCap = sizeCap, subject = subject)
    } else {
        buffer
    }
    writeFileTo(serializer = serializer, file = file, output = output)
    return buffer.toByteArray()
}

private class SizeCappedOutputStream(
    private val delegate: OutputStream,
    private val sizeCap: SizeCap,
    private val subject: String,
) : OutputStream() {
    private var written: Int = 0

    override fun write(byte: Int) {
        advance(count = 1)
        delegate.write(byte)
    }

    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        advance(count = length)
        delegate.write(bytes, offset, length)
    }

    private fun advance(count: Int) {
        if (count > sizeCap.bytes - written) {
            throw largerThanCapException(subject = subject, sizeCap = sizeCap)
        }
        written += count
    }
}
