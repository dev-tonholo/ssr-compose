package dev.tonholo.ssrcompose.catalog.bundle.io

import dev.tonholo.ssrcompose.catalog.bundle.CatalogBundleReader
import dev.tonholo.ssrcompose.catalog.bundle.CatalogFileException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class CatalogManifestWorstShapeMemoryTest {
    @Test
    fun `given a manifest of 16 MiB of tiny entries, when it is read, then it is refused without running out`() {
        // Arrange
        val entry = """{"name":"a","file":"a","fingerprint":"a"}"""
        val manifest = fileAtCap(
            head = """{"format":1,"version":"$NON_LATIN_1","parts":[""",
            element = "$entry,",
            last = entry,
            tail = "]}",
        )

        // Act
        val read = { CatalogBundleReader.read(manifest = manifest, readPart = { error("no part is read") }) }

        // Assert
        assertFailsWith<CatalogFileException> { read() }
    }

    private companion object {
        const val NON_LATIN_1 = "€"
    }
}
