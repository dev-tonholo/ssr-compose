package dev.tonholo.ssrcompose.catalog.bundle

import dev.tonholo.ssrcompose.catalog.bundle.io.requireWithin
import java.io.InputStream
import kotlin.jvm.JvmInline

private const val BYTES_PER_MEBIBYTE = 1024 * 1024
private const val DEFAULT_MEBIBYTES = 16

/**
 * The most bytes a manifest or a part may have. A file over its cap is refused before it is parsed.
 *
 * @throws IllegalArgumentException when [bytes] is negative.
 */
@JvmInline
public value class SizeCap(public val bytes: Int) {
    init {
        require(bytes >= 0) { "A size cap must not be negative" }
    }

    /**
     * Reads all of [stream], and refuses it as soon as it holds more than [bytes]: at most one byte more than
     * the cap is read. The stream is not closed.
     *
     * @param subject what the file is, such as `manifest` or `part app`.
     * @throws CatalogFileException when the stream holds more than [bytes].
     */
    public fun read(stream: InputStream, subject: String): ByteArray =
        stream.readNBytes(readLimit).requireWithin(sizeCap = this, subject = subject)

    private val readLimit: Int
        get() = if (bytes == Int.MAX_VALUE) bytes else bytes + 1

    public companion object {
        /** 16 MiB for each file. */
        public val DEFAULT: SizeCap = SizeCap(bytes = DEFAULT_MEBIBYTES * BYTES_PER_MEBIBYTE)
    }
}
