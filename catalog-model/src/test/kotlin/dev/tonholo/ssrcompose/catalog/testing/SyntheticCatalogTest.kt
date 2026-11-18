package dev.tonholo.ssrcompose.catalog.testing

import dev.tonholo.ssrcompose.catalog.bundle.CatalogBundleReader
import dev.tonholo.ssrcompose.catalog.bundle.CatalogBundleWriter
import dev.tonholo.ssrcompose.catalog.model.Catalog
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.core.data.ValueKind
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SyntheticCatalogTest {
    @Test
    fun `given a shape, when the catalog is generated, then it holds the asked components and parts`() {
        // Arrange
        val shape = shapeOf(palette = ParameterPalette.Full)

        // Act
        val catalog = SyntheticCatalog.generate(shape = shape)

        // Assert
        assertEquals(expected = COMPONENTS, actual = catalog.components().size)
        assertEquals(expected = PARTS, actual = catalog.parts.size)
    }

    @Test
    fun `given a shape, when the catalog is generated, then most components have 2 to 4 parameters`() {
        // Arrange
        val shape = shapeOf(palette = ParameterPalette.Full)

        // Act
        val counts = SyntheticCatalog.generate(shape = shape).components().map { it.parameters.size }

        // Assert
        val common = counts.count { it in COMMON_PARAMETERS }
        assertTrue(actual = common > counts.size / 2, message = "$common of ${counts.size} have 2 to 4 parameters")
    }

    @Test
    fun `given a shape, when the catalog is generated, then a few components have 15 or more parameters`() {
        // Arrange
        val shape = shapeOf(palette = ParameterPalette.Full)

        // Act
        val counts = SyntheticCatalog.generate(shape = shape).components().map { it.parameters.size }

        // Assert
        val large = counts.count { it >= LARGE_PARAMETERS }
        assertTrue(actual = large in 1..(counts.size / FEW_DIVISOR), message = "$large of ${counts.size} are large")
    }

    @Test
    fun `given a shape, when the catalog is generated, then components average about 4 parameters`() {
        // Arrange
        val shape = shapeOf(palette = ParameterPalette.Full)

        // Act
        val counts = SyntheticCatalog.generate(shape = shape).components().map { it.parameters.size }

        // Assert
        val average = counts.average()
        assertTrue(actual = average in AVERAGE_PARAMETERS, message = "The average is $average")
    }

    @Test
    fun `given the same seed, when the catalog is generated twice, then every file has the same bytes`() {
        // Arrange
        val shape = shapeOf(palette = ParameterPalette.Full)

        // Act
        val first = CatalogBundleWriter.write(catalog = SyntheticCatalog.generate(shape = shape))
        val second = CatalogBundleWriter.write(catalog = SyntheticCatalog.generate(shape = shape))

        // Assert
        assertContentEquals(expected = first.manifest, actual = second.manifest)
    }

    @Test
    fun `given two seeds, when the catalog is generated, then the bundles differ`() {
        // Arrange
        val shape = shapeOf(palette = ParameterPalette.Full)
        val otherShape = shape.copy(seed = OTHER_SEED)

        // Act
        val first = CatalogBundleWriter.write(catalog = SyntheticCatalog.generate(shape = shape))
        val second = CatalogBundleWriter.write(catalog = SyntheticCatalog.generate(shape = otherShape))

        // Assert
        assertFalse(actual = first.manifest.contentEquals(second.manifest))
    }

    @Test
    fun `given a generated catalog, when its bundle is read back, then it is valid and equal`() {
        // Arrange
        val catalog = SyntheticCatalog.generate(shape = shapeOf(palette = ParameterPalette.Full))
        val files = CatalogBundleWriter.write(catalog = catalog)

        // Act
        val read = CatalogBundleReader.read(manifest = files.manifest, readPart = files.parts::getValue)

        // Assert
        assertEquals(expected = catalog.components().toSet(), actual = read.components().toSet())
    }

    @Test
    fun `given the full palette, when the catalog is generated, then it uses every group of parameter`() {
        // Arrange
        val shape = shapeOf(palette = ParameterPalette.Full)

        // Act
        val parameters = SyntheticCatalog.generate(shape = shape).components().flatMap { it.parameters }

        // Assert
        val values = parameters.filterIsInstance<Parameter.Value>().map { it.kind }
        assertTrue(actual = values.any { it is ValueKind.Choice }, message = "No choice parameter")
        assertTrue(actual = values.any { it is ValueKind.ListOf }, message = "No list parameter")
        assertTrue(actual = parameters.any { it is Parameter.Callback }, message = "No callback parameter")
        assertTrue(actual = parameters.any { it is Parameter.Slot }, message = "No slot parameter")
    }

    @Test
    fun `given the bridge palette, when the catalog is generated, then every parameter is one a bridge can create`() {
        // Arrange
        val shape = shapeOf(palette = ParameterPalette.BridgeExperiment)

        // Act
        val parameters = SyntheticCatalog.generate(shape = shape).components().flatMap { it.parameters }

        // Assert
        val unsupported = parameters.filterNot(::isSupportedByTheBridgeExperiment)
        assertEquals(expected = emptyList(), actual = unsupported)
    }

    private fun isSupportedByTheBridgeExperiment(parameter: Parameter): Boolean = when (parameter) {
        is Parameter.Value -> parameter.kind == ValueKind.Text && parameter.nullable == !parameter.required
        is Parameter.Callback -> parameter.required && parameter.arguments == listOf(ValueKind.Text)
        is Parameter.Slot -> parameter.arguments.isEmpty()
    }

    private fun shapeOf(palette: ParameterPalette) = SyntheticCatalogShape(
        components = COMPONENTS,
        parts = PARTS,
        seed = SEED,
        palette = palette,
    )

    private fun Catalog.components(): List<ComponentDescriptor> = parts.flatMap { it.components }

    private companion object {
        const val COMPONENTS = 2_000
        const val PARTS = 5
        const val SEED = 20_000L
        const val OTHER_SEED = 80_000L
        const val LARGE_PARAMETERS = 15
        const val FEW_DIVISOR = 10
        val COMMON_PARAMETERS = 2..4
        val AVERAGE_PARAMETERS = 3.5..4.5
    }
}
