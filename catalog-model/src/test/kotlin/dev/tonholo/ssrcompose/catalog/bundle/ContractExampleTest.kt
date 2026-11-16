package dev.tonholo.ssrcompose.catalog.bundle

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals

class ContractExampleTest {
    @Test
    fun `given the example catalog of the contract, when it is written, then the files are the example`() {
        // Arrange
        val catalog = contractExampleCatalog()

        // Act
        val written = CatalogBundleWriter.write(catalog = catalog)

        // Assert
        val expected = mapOf(MANIFEST_FILE_NAME to written.manifest.decodeToString()) +
            written.parts.mapValues { (_, bytes) -> bytes.decodeToString() }
        assertEquals(expected = expected, actual = filesInDocument())
    }

    @Test
    fun `given the files of the contract example, when the bundle is read, then it is the example catalog`() {
        // Arrange
        val files = filesInDocument().mapValues { (_, text) -> text.encodeToByteArray() }

        // Act
        val actual = CatalogBundleReader.read(
            manifest = files.getValue(key = MANIFEST_FILE_NAME),
            readPart = { fileName -> files.getValue(key = fileName) },
        )

        // Assert
        assertEquals(expected = contractExampleCatalog(), actual = actual)
    }

    private fun filesInDocument(): Map<String, String> {
        val markdown = Files.readString(Path.of(System.getProperty(CONTRACT_DOCUMENT_PROPERTY)))
        return FILE_BLOCK.findAll(input = markdown).associate { match -> match.groupValues[1] to match.groupValues[2] }
    }

    private companion object {
        const val CONTRACT_DOCUMENT_PROPERTY = "ssrcompose.contract.document"
        const val MANIFEST_FILE_NAME = "manifest.json"
        val FILE_BLOCK = Regex(pattern = "`([^`]+)`:\\n\\n```json\\n(.*?\\n)```", option = RegexOption.DOT_MATCHES_ALL)
    }
}
