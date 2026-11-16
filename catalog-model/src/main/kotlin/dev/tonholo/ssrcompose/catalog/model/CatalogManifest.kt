package dev.tonholo.ssrcompose.catalog.model

import dev.tonholo.ssrcompose.core.catalog.CatalogVersion

/**
 * The first file of a catalog: its [version] and one entry for each part, with the fingerprint that part must
 * have. Its order does not matter here: the manifest file has the entries sorted by name.
 *
 * @throws IllegalArgumentException when a part name is repeated.
 */
public data class CatalogManifest(public val version: CatalogVersion, public val parts: List<PartEntry>) {
    init {
        require(parts.map { it.name }.toSet().size == parts.size) { "A manifest must not repeat a part name" }
    }

    public companion object {
        public const val FILE_NAME: String = "manifest.json"
    }
}
