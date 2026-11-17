package dev.tonholo.ssrcompose.catalog.bundle

import dev.tonholo.ssrcompose.catalog.bundle.io.CatalogManifestReader
import dev.tonholo.ssrcompose.catalog.model.Catalog
import dev.tonholo.ssrcompose.catalog.model.CatalogManifest
import dev.tonholo.ssrcompose.catalog.model.PartEntry
import dev.tonholo.ssrcompose.catalog.model.PartName
import dev.tonholo.ssrcompose.core.catalog.CatalogVersion

internal const val EVERY_KIND_MANIFEST_RESOURCE = "golden/manifest.json"
internal const val EVERY_KIND_PART_FINGERPRINT = "642a1ffc374431f6e07f96bd63a9d968f8424688cf4ed6cd7df7a8fa54e65d1b"

internal const val EVERY_KIND_CATALOG_FINGERPRINT = "ea757c870b4487a017a8db18e23c698a79adf0802d8911d8da2a68a165116d91"

internal const val FINGERPRINT_LENGTH = 64

internal val ANY_FINGERPRINT: String = "a".repeat(n = FINGERPRINT_LENGTH)

internal fun readManifest(text: String): CatalogManifest =
    CatalogManifestReader.read(bytes = text.encodeToByteArray(), sizeCap = SizeCap.DEFAULT)

internal fun entryText(name: String, file: String = "$name.part.json", fingerprint: String = ANY_FINGERPRINT): String =
    // language=json
    """{"name":"$name","file":"$file","fingerprint":"$fingerprint"}"""

internal fun manifestText(vararg entries: String, version: String = "1"): String =
    // language=json
    """{"format":1,"version":"$version","parts":[${entries.joinToString(separator = ",")}]}"""

internal fun everyKindManifest(): CatalogManifest = CatalogManifest(
    version = CatalogVersion(value = "v \"1\" \\ ü"),
    parts = listOf(PartEntry(name = PartName(value = "every-kind"), fingerprint = EVERY_KIND_PART_FINGERPRINT)),
)

internal fun everyKindCatalog(): Catalog = Catalog(
    version = everyKindManifest().version,
    parts = listOf(everyKindPart()),
)

internal fun manifestWithEntries(vararg names: String): CatalogManifest = CatalogManifest(
    version = CatalogVersion(value = "1"),
    parts = names.map { name -> PartEntry(name = PartName(value = name), fingerprint = ANY_FINGERPRINT) },
)
