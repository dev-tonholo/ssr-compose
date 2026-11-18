package dev.tonholo.ssrcompose.tooling

import dev.tonholo.ssrcompose.catalog.bundle.CatalogBundleReader
import dev.tonholo.ssrcompose.catalog.bundle.CatalogFileException
import dev.tonholo.ssrcompose.catalog.bundle.SizeCap
import dev.tonholo.ssrcompose.catalog.index.CatalogIndexReader
import dev.tonholo.ssrcompose.catalog.index.CatalogIndexWriter
import dev.tonholo.ssrcompose.catalog.model.CatalogManifest
import java.io.File
import java.io.IOException

/** Turns a saved catalog into the index the compiler plugin reads. */
public object CatalogIndexStep {
    /**
     * Reads and checks the catalog saved in [catalogFolder], then writes its index to [indexFile]. The bytes depend
     * only on the catalog, so the same catalog in another folder gives the same index.
     *
     * @param sizeCap the most bytes the manifest and each part of the saved catalog may have.
     * @throws CatalogFileException when the saved catalog is not valid, or its index is over the cap the compiler
     * plugin reads with, [CatalogIndexReader.DEFAULT_SIZE_CAP]. No index is written then.
     * @throws IOException when a file of the saved catalog is missing or cannot be read, or the index cannot be
     * written.
     */
    public fun run(catalogFolder: File, indexFile: File, sizeCap: SizeCap = SizeCap.DEFAULT) {
        writeIndex(
            catalogFolder = catalogFolder,
            indexFile = indexFile,
            sizeCap = sizeCap,
            indexSizeCap = CatalogIndexReader.DEFAULT_SIZE_CAP,
        )
    }

    internal fun writeIndex(catalogFolder: File, indexFile: File, sizeCap: SizeCap, indexSizeCap: SizeCap) {
        val catalog = catalogFolder.resolve(CatalogManifest.FILE_NAME).inputStream().use { manifest ->
            CatalogBundleReader.read(
                manifest = manifest,
                openPart = { fileName -> catalogFolder.resolve(fileName).inputStream() },
                sizeCap = sizeCap,
            )
        }
        val index = CatalogIndexWriter.write(catalog = catalog, sizeCap = indexSizeCap)
        indexFile.parentFile?.mkdirs()
        indexFile.writeBytes(array = index)
    }
}
