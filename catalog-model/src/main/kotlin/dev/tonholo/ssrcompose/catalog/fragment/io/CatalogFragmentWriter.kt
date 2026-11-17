package dev.tonholo.ssrcompose.catalog.fragment.io

import dev.tonholo.ssrcompose.catalog.bundle.CatalogFileException
import dev.tonholo.ssrcompose.catalog.bundle.SizeCap
import dev.tonholo.ssrcompose.catalog.bundle.io.writeFile
import dev.tonholo.ssrcompose.catalog.bundle.io.writeFileTo
import dev.tonholo.ssrcompose.catalog.fragment.CatalogFragment
import dev.tonholo.ssrcompose.catalog.fragment.mapping.toFragmentFile
import dev.tonholo.ssrcompose.catalog.fragment.serialization.CatalogFragmentFile
import dev.tonholo.ssrcompose.catalog.validation.cutForMessage
import java.io.OutputStream

/**
 * Writes a fragment as one file. The same fragment always gives the same bytes, with its components sorted by
 * name, in the same canonical form as a part file.
 */
public object CatalogFragmentWriter {
    /**
     * @param sizeCap the most bytes the file may have. The default is the cap of a part file.
     * @throws CatalogFileException when the file would be larger than
     * [sizeCap]. The message names the fragment.
     */
    public fun write(fragment: CatalogFragment, sizeCap: SizeCap = SizeCap.DEFAULT): ByteArray =
        writeFile(
            serializer = CatalogFragmentFile.serializer(),
            file = fragment.toFragmentFile(),
            subject = "fragment ${fragment.source.cutForMessage()}",
            sizeCap = sizeCap,
        )

    internal fun writeTo(fragment: CatalogFragment, output: OutputStream) {
        writeFileTo(
            serializer = CatalogFragmentFile.serializer(),
            file = fragment.toFragmentFile(),
            output = output,
        )
    }
}
