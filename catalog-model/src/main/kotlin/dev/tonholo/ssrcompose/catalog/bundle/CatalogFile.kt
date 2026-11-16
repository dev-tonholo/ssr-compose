package dev.tonholo.ssrcompose.catalog.bundle

import dev.tonholo.ssrcompose.catalog.model.Catalog
import dev.tonholo.ssrcompose.catalog.validation.CatalogLimits

/**
 * Reads and writes the catalog file, format version [Catalog.CURRENT_FORMAT].
 *
 * The same catalog always gives the same text: fixed key order, components and choice lists sorted by name,
 * two-space indent and one trailing newline.
 */
public object CatalogFile {
    /**
     * Text longer than [maxLength] characters is refused before it is parsed. The default is 16 MiB.
     *
     * A 16 MiB catalog may need up to 320 MiB of heap. The worst shape is one choice list of one-letter
     * entries: it fails at 256 MiB and works at 320 MiB. Set a smaller [maxLength] when less heap is available.
     *
     * @throws CatalogFileException when the text is too long, is not a valid catalog file, or its format is not known.
     */
    public fun read(text: String, maxLength: Int = CatalogLimits.MAX_FILE_LENGTH): Catalog =
        CatalogFileReader.read(text = text, maxLength = maxLength)

    public fun write(catalog: Catalog): String = CatalogFileWriter.write(catalog = catalog)
}
