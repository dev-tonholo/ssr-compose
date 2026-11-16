package dev.tonholo.ssrcompose.catalog.bundle

import kotlin.test.Test
import kotlin.test.assertEquals

class CatalogFileGoldenTest {
    @Test
    fun `given a catalog with every kind of parameter, when it is written, then the bytes are the golden file`() {
        // Arrange
        val catalog = everyKindCatalog()

        // Act
        val actual = CatalogFile.write(catalog = catalog)

        // Assert
        assertEquals(expected = TestResources.text(name = EVERY_KIND_CATALOG_RESOURCE), actual = actual)
    }

    @Test
    fun `given another order, when the catalog is written, then the bytes are the golden file`() {
        // Arrange
        val catalog = everyKindCatalogInAnotherOrder()

        // Act
        val actual = CatalogFile.write(catalog = catalog)

        // Assert
        assertEquals(expected = TestResources.text(name = EVERY_KIND_CATALOG_RESOURCE), actual = actual)
    }

    @Test
    fun `given a catalog with every kind of parameter, when the fingerprint is computed, then it is the pinned one`() {
        // Arrange
        val catalog = everyKindCatalog()

        // Act
        val actual = Fingerprint.of(catalog = catalog)

        // Assert
        assertEquals(expected = EVERY_KIND_CATALOG_FINGERPRINT, actual = actual)
    }

    @Test
    fun `given the golden file, when it is read, then it is the catalog with every kind of parameter`() {
        // Arrange
        val text = TestResources.text(name = EVERY_KIND_CATALOG_RESOURCE)

        // Act
        val actual = CatalogFile.read(text = text)

        // Assert
        assertEquals(expected = everyKindCatalog(), actual = actual)
    }

    private companion object {
        const val EVERY_KIND_CATALOG_FINGERPRINT = "6feb7741fcf2c0f6661563fa81bcb5c32d42b829804fe326b07f300f2ecd042f"
    }
}
