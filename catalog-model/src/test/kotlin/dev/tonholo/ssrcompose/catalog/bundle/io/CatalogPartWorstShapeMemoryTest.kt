package dev.tonholo.ssrcompose.catalog.bundle.io

import dev.tonholo.ssrcompose.catalog.bundle.CatalogBundleReader
import dev.tonholo.ssrcompose.catalog.bundle.CatalogFileException
import dev.tonholo.ssrcompose.catalog.bundle.fingerprint.Fingerprint
import dev.tonholo.ssrcompose.catalog.model.CatalogManifest
import dev.tonholo.ssrcompose.catalog.model.PartEntry
import dev.tonholo.ssrcompose.catalog.model.PartName
import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
import kotlin.test.Test
import kotlin.test.assertFailsWith

class CatalogPartWorstShapeMemoryTest {
    @Test
    fun `given a part of 16 MiB of tiny choice entries, when it is read, then it is refused without running out`() {
        // Arrange
        val part = fileAtCap(
            // language=json
            head = """{"format":1,"part":"app","choices":[{"name":"$NON_LATIN_1","entries":[""",
            // language=json
            element = """"a",""",
            // language=json
            last = """"a"""",
            // language=json
            tail = """]}],"components":[]}""",
        )
        val entry = PartEntry(name = PartName(value = "app"), fingerprint = Fingerprint.of(bytes = part))
        val manifest = CatalogManifest(version = CatalogVersion(value = "1"), parts = listOf(entry))
        val manifestBytes = CatalogManifestWriter.write(manifest = manifest)

        // Act
        val read = { CatalogBundleReader.read(manifest = manifestBytes, readPart = { part }) }

        // Assert
        assertFailsWith<CatalogFileException> { read() }
    }

    private companion object {
        const val NON_LATIN_1 = "€"
    }
}
