package dev.tonholo.ssrcompose.catalog.bundle.io

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
internal fun <F> writeFile(serializer: KSerializer<F>, file: F): ByteArray {
    val output = ByteArrayOutputStream()
    writeFileTo(serializer = serializer, file = file, output = output)
    return output.toByteArray()
}
