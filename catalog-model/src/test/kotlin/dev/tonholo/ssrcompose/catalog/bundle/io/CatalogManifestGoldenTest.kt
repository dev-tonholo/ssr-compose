package dev.tonholo.ssrcompose.catalog.bundle.io

import dev.tonholo.ssrcompose.catalog.bundle.EVERY_KIND_MANIFEST_RESOURCE
import dev.tonholo.ssrcompose.catalog.bundle.SizeCap
import dev.tonholo.ssrcompose.catalog.bundle.TestResources
import dev.tonholo.ssrcompose.catalog.bundle.everyKindManifest
import dev.tonholo.ssrcompose.catalog.bundle.manifestWithEntries
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class CatalogManifestGoldenTest {
    @Test
    fun `given the golden file, when it is read, then it is the manifest with one part`() {
        // Arrange
        val bytes = TestResources.bytes(name = EVERY_KIND_MANIFEST_RESOURCE)

        // Act
        val actual = CatalogManifestReader.read(bytes = bytes, sizeCap = SizeCap.DEFAULT)

        // Assert
        assertEquals(expected = everyKindManifest(), actual = actual)
    }

    @Test
    fun `given parts in another order, when the manifest is written, then the bytes are the same`() {
        // Arrange
        val sorted = manifestWithEntries("app", "feature-home", "feature-payment")
        val unsorted = manifestWithEntries("feature-home", "feature-payment", "app")

        // Act
        val actual = CatalogManifestWriter.write(manifest = unsorted)

        // Assert
        assertContentEquals(expected = CatalogManifestWriter.write(manifest = sorted), actual = actual)
    }
}
