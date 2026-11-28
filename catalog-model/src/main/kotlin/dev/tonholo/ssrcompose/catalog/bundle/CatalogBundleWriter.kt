package dev.tonholo.ssrcompose.catalog.bundle

import dev.tonholo.ssrcompose.catalog.bundle.io.CatalogManifestWriter
import dev.tonholo.ssrcompose.catalog.bundle.io.CatalogPartWriter
import dev.tonholo.ssrcompose.catalog.model.Catalog
import dev.tonholo.ssrcompose.catalog.model.CatalogManifest
import dev.tonholo.ssrcompose.catalog.model.CatalogPart
import dev.tonholo.ssrcompose.catalog.model.PartEntry
import dev.tonholo.ssrcompose.core.fingerprint.Fingerprint

/**
 * Writes a catalog as a manifest and one file for each part.
 *
 * The same catalog always gives the same bytes in every file: fixed key order, parts, components and choice
 * lists sorted by name, two-space indent, one trailing newline, no timestamp and no path. The bytes of a part
 * depend on that part alone, so a change in one module changes only its part and the manifest.
 */
public object CatalogBundleWriter {
    public fun write(catalog: Catalog): CatalogBundleFiles {
        val parts = catalog.parts.sortedBy { it.name.value }.map { part -> WrittenPart(part = part) }
        val manifest = CatalogManifest(version = catalog.version, parts = parts.map(WrittenPart::entry))
        return CatalogBundleFiles(
            manifest = CatalogManifestWriter.write(manifest = manifest),
            parts = parts.associate { written -> written.entry.file to written.bytes },
        )
    }
}

private class WrittenPart(part: CatalogPart) {
    val bytes: ByteArray = CatalogPartWriter.write(part = part)
    val entry: PartEntry = PartEntry(name = part.name, fingerprint = Fingerprint.of(bytes = bytes))
}
