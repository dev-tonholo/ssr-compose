package dev.tonholo.ssrcompose.gradle.testing

import dev.tonholo.ssrcompose.catalog.bundle.CatalogBundleWriter
import dev.tonholo.ssrcompose.catalog.fragment.CatalogFragment
import dev.tonholo.ssrcompose.catalog.fragment.io.CatalogFragmentReader
import dev.tonholo.ssrcompose.catalog.model.Catalog
import dev.tonholo.ssrcompose.catalog.model.CatalogPart
import dev.tonholo.ssrcompose.catalog.model.PartName
import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
import java.io.File

/**
 * The smallest merge of the fragments of one compilation into the module's part. The real merge is the
 * `ssrCatalogPart` step of a later task; this one lets a test compare parts byte for byte.
 */
internal object VariantPart {
    private const val PART_NAME = "fixture"
    private const val VERSION = "1"

    fun fragmentsIn(folder: File): List<CatalogFragment> =
        folder.listFiles().orEmpty().sortedBy { it.name }.map { file ->
            CatalogFragmentReader.read(bytes = file.readBytes(), subject = "fragment ${file.name}")
        }

    /**
     * @throws IllegalArgumentException when two fragments hold the same component name.
     */
    fun partFrom(folder: File): CatalogPart =
        CatalogPart(
            name = PartName(value = PART_NAME),
            choices = emptyList(),
            components = fragmentsIn(folder = folder).flatMap { it.components },
        )

    fun bytesFrom(folder: File): ByteArray {
        val catalog = Catalog(version = CatalogVersion(value = VERSION), parts = listOf(partFrom(folder = folder)))
        return CatalogBundleWriter.write(catalog = catalog).parts.values.single()
    }
}
