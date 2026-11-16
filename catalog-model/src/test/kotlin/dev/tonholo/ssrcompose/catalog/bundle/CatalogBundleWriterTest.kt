package dev.tonholo.ssrcompose.catalog.bundle

import dev.tonholo.ssrcompose.catalog.model.Catalog
import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CatalogBundleWriterTest {
    @Test
    fun `given the catalog of the golden bundle, when it is written, then its fingerprint is the pinned one`() {
        // Arrange
        val catalog = everyKindCatalog()

        // Act
        val actual = CatalogBundleWriter.write(catalog = catalog).fingerprint

        // Assert
        assertEquals(expected = EVERY_KIND_CATALOG_FINGERPRINT, actual = actual)
    }

    @Test
    fun `given a part one byte over the default cap, when the bundle is written, then it is refused naming the part`() {
        // Arrange
        val (_, overCap) = partAtCapPair(name = "app")
        val catalog = Catalog(version = CatalogVersion(value = "1"), parts = listOf(overCap))

        // Act
        val write = { CatalogBundleWriter.write(catalog = catalog) }

        // Assert
        val message = assertFailsWith<CatalogFileException> { write() }.message.orEmpty()
        assertTrue(
            actual = message.contains(other = APP_PART_SUBJECT) &&
                message.contains(other = "larger than $DEFAULT_CAP_BYTES bytes"),
            message = message,
        )
    }
}
