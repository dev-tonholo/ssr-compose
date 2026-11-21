package dev.tonholo.ssrcompose.core.font

import dev.tonholo.ssrcompose.core.font.internal.FontBundleFile
import dev.tonholo.ssrcompose.core.font.internal.fontJson

private const val TRAILING_NEWLINE = "\n"

/**
 * Writes the `fonts.json` of a font bundle in its one form: fixed key order, two-space indent, one trailing
 * newline, no timestamp and no path. The same manifest always gives the same bytes, and [FontBundleReader] reads
 * them back.
 */
public object FontBundleWriter {
    /**
     * @throws FontBundleException when the bytes would be over [FontBundleReader.MANIFEST_SIZE_CAP], which
     * [FontBundleReader.read] refuses by default.
     */
    public fun write(manifest: FontBundleManifest): ByteArray {
        val file = FontBundleFile.of(manifest = manifest)
        val text = fontJson.encodeToString(serializer = FontBundleFile.serializer(), value = file)
        val bytes = (text + TRAILING_NEWLINE).encodeToByteArray()
        if (bytes.size > FontBundleReader.MANIFEST_SIZE_CAP) {
            throw FontBundleException(
                message = "The fonts.json would be larger than ${FontBundleReader.MANIFEST_SIZE_CAP} bytes",
            )
        }
        return bytes
    }
}
