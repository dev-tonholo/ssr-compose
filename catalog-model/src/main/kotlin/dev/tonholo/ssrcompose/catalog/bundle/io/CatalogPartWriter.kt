package dev.tonholo.ssrcompose.catalog.bundle.io

import dev.tonholo.ssrcompose.catalog.bundle.SizeCap
import dev.tonholo.ssrcompose.catalog.bundle.mapping.toPartFile
import dev.tonholo.ssrcompose.catalog.bundle.serialization.file.CatalogPartFile
import dev.tonholo.ssrcompose.catalog.model.CatalogPart
import java.io.OutputStream

internal object CatalogPartWriter {
    fun write(part: CatalogPart): ByteArray = writeFile(
        serializer = CatalogPartFile.serializer(),
        file = part.toPartFile(),
        subject = partSubject(name = part.name),
        sizeCap = SizeCap.DEFAULT,
    )

    fun writeTo(part: CatalogPart, output: OutputStream) {
        writeFileTo(serializer = CatalogPartFile.serializer(), file = part.toPartFile(), output = output)
    }
}
