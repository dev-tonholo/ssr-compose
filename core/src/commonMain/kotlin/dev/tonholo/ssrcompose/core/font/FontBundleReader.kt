package dev.tonholo.ssrcompose.core.font

import dev.tonholo.ssrcompose.core.font.internal.FontBundleFile
import kotlinx.serialization.json.Json

private const val BYTES_PER_MEBIBYTE = 1_048_576
private const val FONT_FILE_MEBIBYTES = 16

/**
 * Reads the `fonts.json` of a font bundle and checks the size of its font files. It does not check fingerprints:
 * the producer and the build tooling hash the files they read.
 */
public object FontBundleReader {
    /** The most bytes a `fonts.json` may have by default. */
    public const val MANIFEST_SIZE_CAP: Int = BYTES_PER_MEBIBYTE

    /** The most bytes a font file may have by default. */
    public const val FONT_FILE_SIZE_CAP: Long = FONT_FILE_MEBIBYTES.toLong() * BYTES_PER_MEBIBYTE

    private val json = Json

    /**
     * Reads a `fonts.json` from [bytes], refusing it before parsing when it is over [sizeCap].
     *
     * @throws FontBundleException when it is over the cap, has a format other than 1, or is not a valid bundle.
     */
    public fun read(bytes: ByteArray, sizeCap: Int = MANIFEST_SIZE_CAP): FontBundleManifest {
        if (bytes.size > sizeCap) {
            throw FontBundleException(message = "The fonts.json is larger than $sizeCap bytes")
        }
        return try {
            json.decodeFromString(deserializer = FontBundleFile.serializer(), string = bytes.decodeToString())
                .toManifest()
        } catch (cause: IllegalArgumentException) {
            throw FontBundleException(message = "The fonts.json is not a valid font bundle", cause = cause)
        }
    }

    /**
     * Refuses a font file of [sizeBytes], taken from the file system before the file is read, when it is over
     * [sizeCap].
     *
     * @throws FontBundleException when the file is over the cap.
     */
    public fun requireFontFileSize(sizeBytes: Long, sizeCap: Long = FONT_FILE_SIZE_CAP) {
        if (sizeBytes > sizeCap) {
            throw FontBundleException(message = "A font file is larger than $sizeCap bytes")
        }
    }
}
