package dev.tonholo.ssrcompose.catalog.bundle

import dev.tonholo.ssrcompose.catalog.bundle.fingerprint.Fingerprint
import dev.tonholo.ssrcompose.catalog.bundle.io.CatalogManifestWriter
import dev.tonholo.ssrcompose.catalog.bundle.io.CatalogPartWriter
import dev.tonholo.ssrcompose.catalog.model.Catalog
import dev.tonholo.ssrcompose.catalog.model.CatalogManifest
import dev.tonholo.ssrcompose.catalog.model.PartEntry
import dev.tonholo.ssrcompose.catalog.model.PartName
import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CatalogBundleReaderTest {
    @Test
    fun `given the golden bundle, when it is read, then it is the catalog of the golden bundle`() {
        // Arrange
        val manifest = TestResources.bytes(name = EVERY_KIND_MANIFEST_RESOURCE)
        val part = TestResources.bytes(name = EVERY_KIND_PART_RESOURCE)
        val source = FakePartSource(files = mapOf("every-kind.part.json" to part))

        // Act
        val actual = CatalogBundleReader.read(manifest = manifest, readPart = source::read)

        // Assert
        assertEquals(expected = everyKindCatalog(), actual = actual)
    }

    @Test
    fun `given a part the manifest does not list, when the bundle is read, then it is never read`() {
        // Arrange
        val files = CatalogBundleWriter.write(catalog = catalogOfParts("home", "app"))
        val source = FakePartSource(files = files.parts + ("extra.part.json" to byteArrayOf()))

        // Act
        readBundle(files = files, source = source)

        // Assert
        assertEquals(expected = listOf("app.part.json", "home.part.json"), actual = source.requested)
    }

    @Test
    fun `given a manifest that breaks a rule, when the bundle is read, then no part file is read`() {
        // Arrange
        val manifest = manifestText(entryText(name = "app", file = "../app.part.json")).encodeToByteArray()
        val source = FakePartSource(files = emptyMap())

        // Act
        val read = { CatalogBundleReader.read(manifest = manifest, readPart = source::read) }

        // Assert
        assertFailsWith<CatalogFileException> { read() }
        assertEquals(expected = emptyList(), actual = source.requested)
    }

    @Test
    fun `given a part that does not match its fingerprint, when the bundle is read, then it is refused by name`() {
        // Arrange
        val files = CatalogBundleWriter.write(catalog = catalogOfParts("app", "home"))
        val valid = files.parts.getValue(key = "home.part.json").decodeToString()
        val tampered = listOf(valid.replace(oldValue = "Card", newValue = "Cart"), "not json at all")

        // Act
        val messages = tampered.map { text ->
            val source = FakePartSource(files = files.parts + ("home.part.json" to text.encodeToByteArray()))
            runCatching { readBundle(files = files, source = source) }.exceptionOrNull()?.message.orEmpty()
        }

        // Assert
        val named = messages.filter { it.contains(other = "part home") && it.contains(other = "fingerprint") }
        assertEquals(expected = tampered.size, actual = named.size, message = messages.toString())
    }

    @Test
    fun `given a part over the cap, when the bundle is read, then it is refused before its fingerprint is checked`() {
        // Arrange
        val cap = 1_000
        val files = CatalogBundleWriter.write(catalog = catalogOfParts("app", "home"))
        val oversized = "x".repeat(n = cap + 1).encodeToByteArray()
        val source = FakePartSource(files = files.parts + ("home.part.json" to oversized))

        // Act
        val read = { readBundle(files = files, source = source, sizeCap = SizeCap(bytes = cap)) }

        // Assert
        val message = assertFailsWith<CatalogFileException> { read() }.message.orEmpty()
        assertTrue(actual = message.contains(other = "part home") && message.contains(other = "larger than $cap bytes"))
    }

    @Test
    fun `given a manifest over the cap, when the bundle is read, then it is refused before it is parsed`() {
        // Arrange
        val cap = 1_000
        val manifest = "x".repeat(n = cap + 1).encodeToByteArray()
        val source = FakePartSource(files = emptyMap())

        // Act
        val read = {
            CatalogBundleReader.read(manifest = manifest, readPart = source::read, sizeCap = SizeCap(bytes = cap))
        }

        // Assert
        val message = assertFailsWith<CatalogFileException> { read() }.message.orEmpty()
        assertTrue(actual = message.contains(other = "larger than $cap bytes"), message = message)
    }

    @Test
    fun `given a manifest that is not valid, when the bundle is read, then the message names the manifest`() {
        // Arrange
        val manifest = """{"format":1,"version":"1","parts":[],"extra":1}""".encodeToByteArray()

        // Act
        val read = { CatalogBundleReader.read(manifest = manifest, readPart = { error("no part is read") }) }

        // Assert
        val message = assertFailsWith<CatalogFileException> { read() }.message.orEmpty()
        assertTrue(actual = message.startsWith(prefix = "The manifest is not valid"), message = message)
    }

    @Test
    fun `given parts of 16 MiB and one byte more, when the bundle is read, then only the first is accepted`() {
        // Arrange
        val (atCapPart, _) = partAtCapPair(name = "app")
        val atCap = CatalogPartWriter.write(part = atCapPart)
        val overCap = atCap + byteArrayOf(TRAILING_SPACE)

        // Act
        val accepted = readListed(name = "app", file = atCap)
        val refused = { readListed(name = "app", file = overCap) }

        // Assert
        assertEquals(expected = listOf("app"), actual = accepted.parts.map { it.name.value })
        val message = assertFailsWith<CatalogFileException> { refused() }.message.orEmpty()
        assertTrue(actual = message.contains(other = "larger than $DEFAULT_CAP_BYTES bytes"), message = message)
    }

    @Test
    fun `given the file of another part, when the bundle is read, then it is refused naming both`() {
        // Arrange
        val home = CatalogPartWriter.write(part = partWith(name = "home"))

        // Act
        val read = { readListed(name = "app", file = home) }

        // Assert
        val message = assertFailsWith<CatalogFileException> { read() }.message.orEmpty()
        assertTrue(actual = message.contains(other = "part app"), message = message)
        assertTrue(actual = message.contains(other = "part home"), message = message)
    }

    @Test
    fun `given a part that is not valid, when the bundle is read, then the message names the part`() {
        // Arrange
        val invalid = """{"format":1,"part":"home","choices":[],"components":[],"extra":1}""".encodeToByteArray()

        // Act
        val read = { readListed(name = "home", file = invalid) }

        // Assert
        val message = assertFailsWith<CatalogFileException> { read() }.message.orEmpty()
        assertTrue(actual = message.startsWith(prefix = "The part home is not valid"), message = message)
    }

    @Test
    fun `given a component name used in two parts, when the bundle is read, then it is refused naming both`() {
        // Arrange
        val files = bundleOfParts(
            partWithComponent(name = "app", component = "Card"),
            partWithComponent(name = "home", component = "Card"),
        )

        // Act
        val read = { readBundle(files = files) }

        // Assert
        val message = assertFailsWith<CatalogFileException> { read() }.message.orEmpty()
        assertTrue(actual = message.contains(other = "app") && message.contains(other = "home"), message = message)
    }

    @Test
    fun `given a part file that cannot be read, when the bundle is read, then the exception passes through`() {
        // Arrange
        val files = CatalogBundleWriter.write(catalog = catalogOfParts("app"))

        // Act
        val read = { CatalogBundleReader.read(manifest = files.manifest, readPart = { throw FileNotFoundException() }) }

        // Assert
        assertFailsWith<FileNotFoundException> { read() }
    }

    @Test
    fun `given the golden bundle as streams, when it is read, then it is the catalog of the golden bundle`() {
        // Arrange
        val manifest = TestResources.bytes(name = EVERY_KIND_MANIFEST_RESOURCE)
        val part = TestResources.bytes(name = EVERY_KIND_PART_RESOURCE)
        val source = FakePartSource(files = mapOf("every-kind.part.json" to part))

        // Act
        val actual = CatalogBundleReader.read(
            manifest = ByteArrayInputStream(manifest),
            openPart = { fileName -> ByteArrayInputStream(source.read(fileName = fileName)) },
        )

        // Assert
        assertEquals(expected = everyKindCatalog(), actual = actual)
    }

    @Test
    fun `given a manifest stream over cap, when the bundle is read, then it stops at cap`() {
        // Arrange
        val cap = 1_000
        val stream = EndlessStream()
        val source = FakePartSource(files = emptyMap())

        // Act
        val read = {
            CatalogBundleReader.read(
                manifest = stream,
                openPart = { fileName -> ByteArrayInputStream(source.read(fileName = fileName)) },
                sizeCap = SizeCap(bytes = cap),
            )
        }

        // Assert
        val message = assertFailsWith<CatalogFileException> { read() }.message.orEmpty()
        assertTrue(actual = message.contains(other = "manifest") && message.contains(other = "larger than $cap bytes"))
        assertTrue(actual = stream.handedOut <= cap + 1, message = "${stream.handedOut} bytes were read")
        assertEquals(expected = emptyList(), actual = source.requested)
    }

    @Test
    fun `given a part stream over cap, when the bundle is read, then it stops at cap`() {
        // Arrange
        val cap = 1_000
        val files = CatalogBundleWriter.write(catalog = catalogOfParts("app"))
        val stream = EndlessStream()

        // Act
        val read = {
            CatalogBundleReader.read(
                manifest = ByteArrayInputStream(files.manifest),
                openPart = { stream },
                sizeCap = SizeCap(bytes = cap),
            )
        }

        // Assert
        val message = assertFailsWith<CatalogFileException> { read() }.message.orEmpty()
        assertTrue(actual = message.contains(other = "part app") && message.contains(other = "larger than $cap bytes"))
        assertTrue(actual = stream.handedOut <= cap + 1, message = "${stream.handedOut} bytes were read")
    }

    @Test
    fun `given a catalog with no part, when it is written and read, then it has no part and no file is read`() {
        // Arrange
        val catalog = Catalog(version = CatalogVersion(value = "1"), parts = emptyList())
        val files = CatalogBundleWriter.write(catalog = catalog)
        val source = FakePartSource(files = emptyMap())

        // Act
        val actual = readBundle(files = files, source = source)

        // Assert
        assertEquals(expected = catalog, actual = actual)
        assertEquals(expected = emptyList(), actual = source.requested)
    }

    private fun readListed(name: String, file: ByteArray): Catalog {
        val entry = PartEntry(name = PartName(value = name), fingerprint = Fingerprint.of(bytes = file))
        val manifest = CatalogManifest(version = CatalogVersion(value = "1"), parts = listOf(entry))
        val source = FakePartSource(files = mapOf(entry.file to file))
        return CatalogBundleReader.read(
            manifest = CatalogManifestWriter.write(manifest = manifest),
            readPart = source::read,
        )
    }

    private fun catalogOfParts(vararg names: String): Catalog = Catalog(
        version = CatalogVersion(value = "1"),
        parts = names.map { name -> partWithComponent(name = name, component = "Card$name") },
    )

    private companion object {
        const val TRAILING_SPACE: Byte = 32
    }
}
