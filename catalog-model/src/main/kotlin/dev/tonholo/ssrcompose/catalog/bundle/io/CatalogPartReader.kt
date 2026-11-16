package dev.tonholo.ssrcompose.catalog.bundle.io

import dev.tonholo.ssrcompose.catalog.bundle.SizeCap
import dev.tonholo.ssrcompose.catalog.bundle.mapping.toPart
import dev.tonholo.ssrcompose.catalog.bundle.serialization.file.CatalogPartFile
import dev.tonholo.ssrcompose.catalog.model.CatalogPart
import dev.tonholo.ssrcompose.catalog.model.PartName

internal fun partSubject(name: PartName): String = "part ${name.value}"

internal object CatalogPartReader {
    fun read(bytes: ByteArray, sizeCap: SizeCap, subject: String): CatalogPart = readFile(
        subject = subject,
        bytes = bytes,
        sizeCap = sizeCap,
        serializer = CatalogPartFile.serializer(),
        toModel = CatalogPartFile::toPart,
        writeModel = CatalogPartWriter::writeTo,
    )
}
