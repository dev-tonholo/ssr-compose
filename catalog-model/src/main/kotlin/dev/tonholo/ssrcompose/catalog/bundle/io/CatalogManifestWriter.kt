package dev.tonholo.ssrcompose.catalog.bundle.io

import dev.tonholo.ssrcompose.catalog.bundle.mapping.toManifestFile
import dev.tonholo.ssrcompose.catalog.bundle.serialization.file.CatalogManifestFile
import dev.tonholo.ssrcompose.catalog.model.CatalogManifest
import java.io.OutputStream

internal object CatalogManifestWriter {
    fun write(manifest: CatalogManifest): ByteArray =
        writeFile(serializer = CatalogManifestFile.serializer(), file = manifest.toManifestFile())

    fun writeTo(manifest: CatalogManifest, output: OutputStream) {
        writeFileTo(serializer = CatalogManifestFile.serializer(), file = manifest.toManifestFile(), output = output)
    }
}
