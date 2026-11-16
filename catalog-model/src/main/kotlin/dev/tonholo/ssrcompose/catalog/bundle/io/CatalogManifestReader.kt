package dev.tonholo.ssrcompose.catalog.bundle.io

import dev.tonholo.ssrcompose.catalog.bundle.SizeCap
import dev.tonholo.ssrcompose.catalog.bundle.mapping.toManifest
import dev.tonholo.ssrcompose.catalog.bundle.serialization.file.CatalogManifestFile
import dev.tonholo.ssrcompose.catalog.model.CatalogManifest

internal const val MANIFEST_SUBJECT = "manifest"

internal object CatalogManifestReader {
    fun read(bytes: ByteArray, sizeCap: SizeCap): CatalogManifest = readFile(
        subject = MANIFEST_SUBJECT,
        bytes = bytes.requireWithin(sizeCap = sizeCap, subject = MANIFEST_SUBJECT),
        serializer = CatalogManifestFile.serializer(),
        toModel = CatalogManifestFile::toManifest,
        writeModel = CatalogManifestWriter::writeTo,
    )
}
