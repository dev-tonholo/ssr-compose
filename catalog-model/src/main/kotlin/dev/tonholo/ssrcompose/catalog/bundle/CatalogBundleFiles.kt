package dev.tonholo.ssrcompose.catalog.bundle

import dev.tonholo.ssrcompose.catalog.model.CatalogManifest
import dev.tonholo.ssrcompose.core.fingerprint.Fingerprint

/**
 * The bytes of every file of a catalog bundle: [manifest], saved as [CatalogManifest.FILE_NAME], and [parts],
 * keyed by the file name the manifest gives each part.
 */
public class CatalogBundleFiles(public val manifest: ByteArray, public val parts: Map<String, ByteArray>) {
    /** The fingerprint of the whole catalog: the SHA-256 of the manifest. It covers every part. */
    public val fingerprint: String
        get() = Fingerprint.of(bytes = manifest)
}
