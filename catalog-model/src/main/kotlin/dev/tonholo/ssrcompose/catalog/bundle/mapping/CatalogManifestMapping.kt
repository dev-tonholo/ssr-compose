package dev.tonholo.ssrcompose.catalog.bundle.mapping

import dev.tonholo.ssrcompose.catalog.bundle.serialization.CatalogFormat
import dev.tonholo.ssrcompose.catalog.bundle.serialization.file.CatalogManifestFile
import dev.tonholo.ssrcompose.catalog.bundle.serialization.file.PartEntryFile
import dev.tonholo.ssrcompose.catalog.model.CatalogManifest
import dev.tonholo.ssrcompose.catalog.model.PartEntry
import dev.tonholo.ssrcompose.catalog.model.PartName
import dev.tonholo.ssrcompose.core.catalog.CatalogVersion

internal fun CatalogManifest.toManifestFile(): CatalogManifestFile = CatalogManifestFile(
    format = CatalogFormat.CURRENT,
    version = version.value,
    parts = parts.sortedBy { it.name.value }.map(PartEntry::toFile),
)

internal fun CatalogManifestFile.toManifest(): CatalogManifest = CatalogManifest(
    version = CatalogVersion(value = version),
    parts = parts.map(PartEntryFile::toEntry),
)

private fun PartEntry.toFile(): PartEntryFile =
    PartEntryFile(name = name.value, file = file, fingerprint = fingerprint)

private fun PartEntryFile.toEntry(): PartEntry {
    val entry = PartEntry(name = PartName(value = name), fingerprint = fingerprint)
    require(file == entry.file) { "The file of the part ${entry.name.value} must be ${entry.file}" }
    return entry
}
