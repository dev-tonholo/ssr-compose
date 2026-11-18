package dev.tonholo.ssrcompose.tooling

import dev.tonholo.ssrcompose.catalog.bundle.CatalogBundleFiles
import dev.tonholo.ssrcompose.catalog.bundle.CatalogBundleReader
import dev.tonholo.ssrcompose.catalog.bundle.CatalogBundleWriter
import dev.tonholo.ssrcompose.catalog.model.Catalog
import dev.tonholo.ssrcompose.catalog.testing.ParameterPalette
import dev.tonholo.ssrcompose.catalog.testing.SyntheticCatalog
import dev.tonholo.ssrcompose.catalog.testing.SyntheticCatalogShape
import dev.tonholo.ssrcompose.catalog.testing.measure.MeasurementPlan
import dev.tonholo.ssrcompose.catalog.testing.measure.SCALE_TAG
import dev.tonholo.ssrcompose.catalog.testing.measure.ScaleMeasurement
import org.junit.jupiter.api.Tag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CatalogScaleTest {
    @Test
    fun `given a small bundle, when it is read and validated, then it holds every component within the cap`() {
        // Arrange
        val shape = shapeOf(components = SMALL_COMPONENTS, parts = SMALL_PARTS)
        val files = CatalogBundleWriter.write(catalog = SyntheticCatalog.generate(shape = shape))

        // Act
        val read = readBundle(files = files)

        // Assert
        assertEquals(expected = SMALL_COMPONENTS, actual = read.componentCount())
        assertEquals(expected = SMALL_PARTS, actual = read.parts.size)
        assertEveryFileWithinCap(files = files)
    }

    @Tag(SCALE_TAG)
    @Test
    fun `given the full bundle, when it is read and validated, then it holds every component within the cap`() {
        // Arrange
        val shape = shapeOf(components = FULL_COMPONENTS, parts = FULL_PARTS)
        val files = CatalogBundleWriter.write(catalog = SyntheticCatalog.generate(shape = shape))

        // Act
        val report = ScaleMeasurement.measure(label = "read and validate bundle", plan = FULL_PLAN) {
            readBundle(files = files)
        }
        val read = readBundle(files = files)

        // Assert
        println(report.toLine())
        println("SCALE | bundle bytes | total ${files.totalBytes()} | largest file ${files.largestFileBytes()}")
        assertEquals(expected = FULL_COMPONENTS, actual = read.componentCount())
        assertEquals(expected = FULL_PARTS, actual = read.parts.size)
        assertTrue(actual = read.parameterCount() in FULL_PARAMETERS, message = "${read.parameterCount()} parameters")
        assertEveryFileWithinCap(files = files)
    }

    private fun shapeOf(components: Int, parts: Int) = SyntheticCatalogShape(
        components = components,
        parts = parts,
        seed = SEED,
        palette = ParameterPalette.Full,
    )

    private fun readBundle(files: CatalogBundleFiles): Catalog =
        CatalogBundleReader.read(manifest = files.manifest, readPart = files.parts::getValue)

    private fun assertEveryFileWithinCap(files: CatalogBundleFiles) {
        assertTrue(actual = files.largestFileBytes() <= FILE_CAP_BYTES, message = "${files.largestFileBytes()} bytes")
    }

    private fun CatalogBundleFiles.largestFileBytes(): Int = (parts.values + manifest).maxOf { it.size }

    private fun CatalogBundleFiles.totalBytes(): Long = (parts.values + manifest).sumOf { it.size.toLong() }

    private fun Catalog.componentCount(): Int = parts.sumOf { it.components.size }

    private fun Catalog.parameterCount(): Int = parts.sumOf { part -> part.components.sumOf { it.parameters.size } }

    private companion object {
        const val SEED = 20_000L
        const val SMALL_COMPONENTS = 500
        const val SMALL_PARTS = 5
        const val FULL_COMPONENTS = 20_000
        const val FULL_PARTS = 50
        const val FILE_CAP_BYTES = 16 * 1024 * 1024
        val FULL_PARAMETERS = 76_000..84_000
        val FULL_PLAN = MeasurementPlan(warmups = 5, runs = 7)
    }
}
