package dev.tonholo.ssrcompose.catalog.bundle.serialization.file

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
internal data class CatalogManifestFile(
    @Contextual val format: Int,
    val version: String,
    val parts: List<PartEntryFile>,
)

@Serializable
internal data class PartEntryFile(val name: String, val file: String, val fingerprint: String)
