package dev.tonholo.ssrcompose.core.font

/**
 * The generated resource that carries the app's font version from the build to the consumer. It holds the
 * version's label as UTF-8 and nothing else.
 */
public object FontVersionResource {
    /** Where the build puts the resource among the app's Java resources. */
    public const val PATH: String = "ssrcompose/font-version"

    public fun write(version: FontBundleVersion): ByteArray = version.value.encodeToByteArray()

    /**
     * Reads the font version from the resource's [bytes].
     *
     * @throws FontBundleException when the bytes are not a font version label.
     */
    public fun read(bytes: ByteArray): FontBundleVersion = try {
        FontBundleVersion(value = bytes.decodeToString())
    } catch (cause: IllegalArgumentException) {
        throw FontBundleException(message = "The font version resource does not hold a font version", cause = cause)
    }
}
