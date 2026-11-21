package dev.tonholo.ssrcompose.core.font

import dev.tonholo.ssrcompose.core.font.internal.FontVersionsFile
import dev.tonholo.ssrcompose.core.font.internal.fontJson

private const val TRAILING_NEWLINE = "\n"

/**
 * The index of the font bundles in the server's resources. Each bundle sits in [FOLDER]/<version>/, and the index
 * at [PATH] lists the versions, sorted, each once.
 */
public object FontVersionsIndex {
    /** The resource folder that holds one folder per font version. */
    public const val FOLDER: String = "ssrcompose/fonts"

    /** Where the index sits among the server's resources, outside [FOLDER] so no version label can name it. */
    public const val PATH: String = "ssrcompose/font-versions.json"

    /** Writes the index of [versions], sorted, with a version that is given more than once listed once. */
    public fun write(versions: List<FontBundleVersion>): ByteArray {
        val file = FontVersionsFile(versions = versions.map { it.value }.distinct().sorted())
        val text = fontJson.encodeToString(serializer = FontVersionsFile.serializer(), value = file)
        return (text + TRAILING_NEWLINE).encodeToByteArray()
    }

    /**
     * Reads the index from [bytes], refusing it before parsing when it is over [sizeCap].
     *
     * @return the versions as the index lists them.
     * @throws FontBundleException when it is over the cap, is not a valid index, names a bad label or repeats one.
     */
    public fun read(bytes: ByteArray, sizeCap: Int = FontBundleReader.MANIFEST_SIZE_CAP): List<FontBundleVersion> {
        if (bytes.size > sizeCap) {
            throw FontBundleException(message = "The font versions index is larger than $sizeCap bytes")
        }
        return try {
            val text = bytes.decodeToString()
            val labels = fontJson.decodeFromString(deserializer = FontVersionsFile.serializer(), string = text).versions
            require(labels.size == labels.toSet().size) { "A font version is listed twice" }
            labels.map { label -> FontBundleVersion(value = label) }
        } catch (cause: IllegalArgumentException) {
            throw FontBundleException(message = "The font versions index is not valid", cause = cause)
        }
    }
}
