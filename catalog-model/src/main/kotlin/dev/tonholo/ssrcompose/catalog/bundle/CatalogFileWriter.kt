package dev.tonholo.ssrcompose.catalog.bundle

import dev.tonholo.ssrcompose.catalog.model.Catalog

private const val TRAILING_NEWLINE = "\n"

internal object CatalogFileWriter {
    fun write(catalog: Catalog): String =
        catalogJson.encodeToString(serializer = CatalogDocument.serializer(), value = catalog.toDocument()) +
            TRAILING_NEWLINE
}
