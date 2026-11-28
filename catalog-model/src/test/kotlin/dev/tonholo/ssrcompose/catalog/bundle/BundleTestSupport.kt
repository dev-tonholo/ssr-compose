package dev.tonholo.ssrcompose.catalog.bundle

import dev.tonholo.ssrcompose.catalog.bundle.io.CatalogManifestWriter
import dev.tonholo.ssrcompose.catalog.bundle.io.CatalogPartWriter
import dev.tonholo.ssrcompose.catalog.model.Catalog
import dev.tonholo.ssrcompose.catalog.model.CatalogManifest
import dev.tonholo.ssrcompose.catalog.model.CatalogPart
import dev.tonholo.ssrcompose.catalog.model.PartEntry
import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
import dev.tonholo.ssrcompose.core.fingerprint.Fingerprint
import java.io.InputStream

private const val STREAM_BYTE: Byte = 120

/**
 * The files of a bundle made from parts as they are, with no check across them.
 */
internal fun bundleOfParts(vararg parts: CatalogPart): CatalogBundleFiles {
    val written = parts.map { part ->
        val bytes = CatalogPartWriter.write(part = part)
        PartEntry(name = part.name, fingerprint = Fingerprint.of(bytes = bytes)) to bytes
    }
    val manifest = CatalogManifest(version = CatalogVersion(value = "1"), parts = written.map { (entry, _) -> entry })
    return CatalogBundleFiles(
        manifest = CatalogManifestWriter.write(manifest = manifest),
        parts = written.associate { (entry, bytes) -> entry.file to bytes },
    )
}

internal fun CatalogBundleFiles.source(): FakePartSource = FakePartSource(files = parts)

internal fun readBundle(
    files: CatalogBundleFiles,
    source: FakePartSource = files.source(),
    sizeCap: SizeCap = SizeCap.DEFAULT,
): Catalog = CatalogBundleReader.read(manifest = files.manifest, readPart = source::read, sizeCap = sizeCap)

internal class EndlessStream : InputStream() {
    var handedOut: Long = 0
        private set

    override fun read(): Int {
        handedOut++
        return STREAM_BYTE.toInt()
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        buffer.fill(element = STREAM_BYTE, fromIndex = offset, toIndex = offset + length)
        handedOut += length
        return length
    }
}
