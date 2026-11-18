package dev.tonholo.ssrcompose.tooling

import dev.tonholo.ssrcompose.catalog.bundle.CatalogBundleWriter
import dev.tonholo.ssrcompose.catalog.bundle.CatalogFileException
import dev.tonholo.ssrcompose.catalog.bundle.SizeCap
import dev.tonholo.ssrcompose.catalog.index.CatalogIndexReader
import dev.tonholo.ssrcompose.catalog.model.Catalog
import dev.tonholo.ssrcompose.catalog.model.CatalogPart
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.catalog.model.PartName
import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
import dev.tonholo.ssrcompose.core.data.ValueKind
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CatalogIndexStepTest {
    @TempDir
    lateinit var firstFolder: File

    @TempDir
    lateinit var secondFolder: File

    @Test
    fun `given a saved catalog, when the step runs, then the index holds its version and components`() {
        // Arrange
        saveCatalog(folder = firstFolder)
        val indexFile = firstFolder.resolve("build/index/$VERSION.index")

        // Act
        CatalogIndexStep.run(catalogFolder = firstFolder, indexFile = indexFile)

        // Assert
        val index = indexFile.inputStream().use { stream -> CatalogIndexReader.read(stream = stream) }
        assertEquals(expected = CatalogVersion(value = VERSION), actual = index.version)
        assertEquals(expected = GREETING, actual = index.component(name = GREETING.name))
    }

    @Test
    fun `given one saved catalog in two folders, when the step runs, then both indexes have the same bytes`() {
        // Arrange
        saveCatalog(folder = firstFolder)
        saveCatalog(folder = secondFolder)
        val firstManifest = firstFolder.resolve(MANIFEST)
        val secondManifest = secondFolder.resolve(MANIFEST)
        secondManifest.setLastModified(EARLIER_TIME)
        assertNotEquals(illegal = firstManifest.lastModified(), actual = secondManifest.lastModified())
        val firstIndex = firstFolder.resolve(INDEX_FILE)
        val secondIndex = secondFolder.resolve(INDEX_FILE)

        // Act
        CatalogIndexStep.run(catalogFolder = firstFolder, indexFile = firstIndex)
        CatalogIndexStep.run(catalogFolder = secondFolder, indexFile = secondIndex)

        // Assert
        assertContentEquals(expected = firstIndex.readBytes(), actual = secondIndex.readBytes())
    }

    @Test
    fun `given a saved catalog, when the step runs, then the index does not hold the folder path`() {
        // Arrange
        val catalogFolder = firstFolder.resolve(NON_ASCII_FOLDER).apply { mkdirs() }
        saveCatalog(folder = catalogFolder)
        val indexFile = firstFolder.resolve(INDEX_FILE)

        // Act
        CatalogIndexStep.run(catalogFolder = catalogFolder, indexFile = indexFile)

        // Assert
        val index = indexFile.readBytes().toString(charset = Charsets.ISO_8859_1)
        val path = catalogFolder.absolutePath.encodeToByteArray().toString(charset = Charsets.ISO_8859_1)
        assertFalse(actual = index.contains(path), message = "The index holds the folder path")
    }

    @Test
    fun `given a valid part that does not match the manifest, when the step runs, then it fails and writes no index`() {
        // Arrange
        saveCatalog(folder = firstFolder)
        val otherPart = CatalogBundleWriter.write(catalog = OTHER_CATALOG).parts.getValue(PART_FILE)
        firstFolder.resolve(PART_FILE).writeBytes(array = otherPart)
        val indexFile = firstFolder.resolve(INDEX_FILE)

        // Act
        assertFailsWith<CatalogFileException> {
            CatalogIndexStep.run(catalogFolder = firstFolder, indexFile = indexFile)
        }

        // Assert
        assertFalse(actual = indexFile.exists())
    }

    @Test
    fun `given a file over the configured cap, when the step runs, then it fails and writes no index`() {
        // Arrange
        saveCatalog(folder = firstFolder)
        val indexFile = firstFolder.resolve(INDEX_FILE)

        // Act
        val failure = assertFailsWith<CatalogFileException> {
            CatalogIndexStep.run(
                catalogFolder = firstFolder,
                indexFile = indexFile,
                sizeCap = SizeCap(bytes = largestSavedFile() - 1),
            )
        }

        // Assert
        assertTrue(actual = failure.message.orEmpty().contains(LARGER_THAN_CAP), message = failure.message)
        assertFalse(actual = indexFile.exists())
    }

    @Test
    fun `given every file within the configured cap, when the step runs, then the index is written`() {
        // Arrange
        saveCatalog(folder = firstFolder)
        val indexFile = firstFolder.resolve(INDEX_FILE)

        // Act
        CatalogIndexStep.run(
            catalogFolder = firstFolder,
            indexFile = indexFile,
            sizeCap = SizeCap(bytes = largestSavedFile()),
        )

        // Assert
        assertTrue(actual = indexFile.exists())
    }

    @Test
    fun `given an index over the cap the compiler reads, when the step runs, then it fails and writes no index`() {
        // Arrange
        saveCatalog(folder = firstFolder)
        val indexFile = firstFolder.resolve(INDEX_FILE)

        // Act
        val failure = assertFailsWith<CatalogFileException> {
            CatalogIndexStep.writeIndex(
                catalogFolder = firstFolder,
                indexFile = indexFile,
                sizeCap = SizeCap.DEFAULT,
                indexSizeCap = SizeCap(bytes = 1),
            )
        }

        // Assert
        assertTrue(actual = failure.message.orEmpty().contains(LARGER_THAN_CAP), message = failure.message)
        assertFalse(actual = indexFile.exists())
    }

    private fun largestSavedFile(): Int {
        val files = CatalogBundleWriter.write(catalog = CATALOG)
        return maxOf(files.manifest.size, files.parts.values.maxOf { it.size })
    }

    private fun saveCatalog(folder: File) {
        val files = CatalogBundleWriter.write(catalog = CATALOG)
        folder.resolve(MANIFEST).writeBytes(array = files.manifest)
        files.parts.forEach { (name, bytes) -> folder.resolve(name).writeBytes(array = bytes) }
    }

    private companion object {
        const val VERSION = "7"
        const val MANIFEST = "manifest.json"
        const val PART_FILE = "app.part.json"
        const val INDEX_FILE = "out/catalog.index"
        const val EARLIER_TIME = 1_000_000L
        const val NON_ASCII_FOLDER = "café"
        const val LARGER_THAN_CAP = "is larger than"
        val GREETING = ComponentDescriptor(
            name = "Greeting",
            parameters = listOf(Parameter.Value(name = "title", kind = ValueKind.Text, required = true)),
        )
        val CATALOG = catalogHolding(component = GREETING)
        val OTHER_CATALOG = catalogHolding(component = ComponentDescriptor(name = "Banner", parameters = emptyList()))

        fun catalogHolding(component: ComponentDescriptor) = Catalog(
            version = CatalogVersion(value = VERSION),
            parts = listOf(
                CatalogPart(name = PartName(value = "app"), choices = emptyList(), components = listOf(component)),
            ),
        )
    }
}
