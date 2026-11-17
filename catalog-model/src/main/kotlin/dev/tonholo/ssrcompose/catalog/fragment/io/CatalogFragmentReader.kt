package dev.tonholo.ssrcompose.catalog.fragment.io

import dev.tonholo.ssrcompose.catalog.bundle.CatalogFileException
import dev.tonholo.ssrcompose.catalog.bundle.SizeCap
import dev.tonholo.ssrcompose.catalog.bundle.io.readFile
import dev.tonholo.ssrcompose.catalog.bundle.io.requireWithin
import dev.tonholo.ssrcompose.catalog.fragment.CatalogFragment
import dev.tonholo.ssrcompose.catalog.fragment.mapping.toFragment
import dev.tonholo.ssrcompose.catalog.fragment.serialization.CatalogFragmentFile

private const val DEFAULT_SUBJECT = "fragment"

/**
 * Reads the fragment file the [CatalogFragmentWriter] wrote.
 */
public object CatalogFragmentReader {
    /**
     * @param subject what the file is, such as `fragment app.Banner.kt.fragment.json`. Every failure message
     * starts with it.
     * @throws CatalogFileException when the file is over [sizeCap], is not valid UTF-8, is not a valid fragment of
     * the known format, or is not in the writer's canonical form.
     */
    public fun read(
        bytes: ByteArray,
        sizeCap: SizeCap = SizeCap.DEFAULT,
        subject: String = DEFAULT_SUBJECT,
    ): CatalogFragment = readFile(
        subject = subject,
        bytes = bytes.requireWithin(sizeCap = sizeCap, subject = subject),
        serializer = CatalogFragmentFile.serializer(),
        toModel = CatalogFragmentFile::toFragment,
        writeModel = CatalogFragmentWriter::writeTo,
    )
}
