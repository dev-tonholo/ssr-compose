package dev.tonholo.ssrcompose.catalog.bundle

import dev.tonholo.ssrcompose.catalog.bundle.io.CatalogManifestReader
import dev.tonholo.ssrcompose.catalog.bundle.io.CatalogPartReader
import dev.tonholo.ssrcompose.catalog.bundle.io.MANIFEST_SUBJECT
import dev.tonholo.ssrcompose.catalog.bundle.io.partSubject
import dev.tonholo.ssrcompose.catalog.bundle.serialization.describe
import dev.tonholo.ssrcompose.catalog.model.Catalog
import dev.tonholo.ssrcompose.catalog.model.CatalogPart
import dev.tonholo.ssrcompose.catalog.model.PartEntry
import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
import java.io.InputStream

/**
 * Reads a catalog from its manifest and its part files, and checks all of it before giving anything back.
 */
public object CatalogBundleReader {
    /**
     * The manifest is read and checked first, and [readPart] is called only for the file names it lists, in its
     * order. Nothing is read for a manifest that fails. A part is refused when it is over [sizeCap], when its
     * bytes do not match its fingerprint in the manifest, when it is not valid, or when it is the file of another
     * part. The checks across parts come last.
     *
     * Only the parts the manifest lists are read. The caller checks the folder for other parts.
     *
     * @param manifest the bytes of the whole manifest file.
     * @param readPart gives the bytes of the part file with the given name, found next to the manifest.
     * @param sizeCap the most bytes the manifest and each part may have. Prefer the stream overload when reading
     * streams so a file over the cap is refused before it is read in full.
     * @throws CatalogFileException when anything is not valid. The message names the manifest or the part, and
     * never repeats more than 64 characters of one piece of text from a file. Nothing is given back for a
     * catalog that fails.
     * An exception thrown by [readPart], such as a missing file or an I/O error, is not changed and passes through.
     */
    public fun read(
        manifest: ByteArray,
        readPart: (fileName: String) -> ByteArray,
        sizeCap: SizeCap = SizeCap.DEFAULT,
    ): Catalog = readFromBytes(manifestBytes = manifest, sizeCap = sizeCap) { entry ->
        readPart(entry.file)
    }

    /**
     * Reads a catalog from [manifest] and streams opened by [openPart], refusing any stream over [sizeCap] before
     * it is read in full. [manifest] is not closed; each stream returned by [openPart] is closed after reading.
     *
     * @param manifest the stream of the manifest file.
     * @param openPart opens the stream of the part file with the given name, found next to the manifest.
     * @param sizeCap the most bytes the manifest and each part may have.
     * @throws CatalogFileException when anything is not valid. Nothing is given back for a catalog that fails.
     * An exception thrown by [openPart], such as a missing file or an I/O error, is not changed and passes through.
     */
    public fun read(
        manifest: InputStream,
        openPart: (fileName: String) -> InputStream,
        sizeCap: SizeCap = SizeCap.DEFAULT,
    ): Catalog = readFromBytes(
        manifestBytes = sizeCap.read(stream = manifest, subject = MANIFEST_SUBJECT),
        sizeCap = sizeCap,
    ) { entry ->
        openPart(entry.file).use { stream ->
            sizeCap.read(stream = stream, subject = partSubject(name = entry.name))
        }
    }

    private inline fun readFromBytes(
        manifestBytes: ByteArray,
        sizeCap: SizeCap,
        readPartBytes: (PartEntry) -> ByteArray,
    ): Catalog {
        val checked = CatalogManifestReader.read(bytes = manifestBytes, sizeCap = sizeCap)
        val parts = checked.parts.map { entry ->
            CatalogPartReader.read(entry = entry, bytes = readPartBytes(entry), sizeCap = sizeCap)
        }
        return catalogOf(version = checked.version, parts = parts)
    }
}

private fun catalogOf(version: CatalogVersion, parts: List<CatalogPart>): Catalog = try {
    Catalog(version = version, parts = parts)
} catch (exception: IllegalArgumentException) {
    throw CatalogFileException(message = "The catalog is not valid: ${describe(exception = exception)}")
}
