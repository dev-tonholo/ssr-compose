package dev.tonholo.ssrcompose.catalog.bundle.io

import dev.tonholo.ssrcompose.catalog.bundle.ANY_FINGERPRINT
import dev.tonholo.ssrcompose.catalog.bundle.CatalogFileException
import dev.tonholo.ssrcompose.catalog.bundle.FINGERPRINT_LENGTH
import dev.tonholo.ssrcompose.catalog.bundle.SizeCap
import dev.tonholo.ssrcompose.catalog.bundle.entryText
import dev.tonholo.ssrcompose.catalog.bundle.manifestText
import dev.tonholo.ssrcompose.catalog.bundle.manifestWithEntries
import dev.tonholo.ssrcompose.catalog.bundle.readManifest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CatalogManifestReaderTest {
    @Test
    fun `given a file name that is not the part name and the suffix, when the manifest is read, then it is refused`() {
        // Arrange
        val files = listOf(
            "", ".", "..", "manifest.json", "../app.part.json", "a/app.part.json", "https://x/app.part.json",
            "app.json", "App.part.json", "other.part.json",
        )

        // Act
        val notRefused = files.filter { file ->
            val text = manifestText(entryText(name = "app", file = file))
            runCatching { readManifest(text = text) }.exceptionOrNull() !is CatalogFileException
        }

        // Assert
        assertEquals(expected = emptyList(), actual = notRefused)
    }

    @Test
    fun `given two entries with the same part name, when the manifest is read, then it is refused`() {
        // Arrange
        val text = manifestText(entryText(name = "app"), entryText(name = "app"))

        // Act
        val read = { readManifest(text = text) }

        // Assert
        assertFailsWith<CatalogFileException> { read() }
    }

    @Test
    fun `given a fingerprint that is not 64 lowercase hexadecimal digits, when the manifest is read, then refused`() {
        // Arrange
        val fingerprints = listOf(
            "",
            "a".repeat(n = FINGERPRINT_LENGTH - 1),
            "a".repeat(n = FINGERPRINT_LENGTH + 1),
            "A".repeat(n = FINGERPRINT_LENGTH),
            "g".repeat(n = FINGERPRINT_LENGTH),
        )

        // Act
        val notRefused = fingerprints.filter { fingerprint ->
            val text = manifestText(entryText(name = "app", fingerprint = fingerprint))
            runCatching { readManifest(text = text) }.exceptionOrNull() !is CatalogFileException
        }

        // Assert
        assertEquals(expected = emptyList(), actual = notRefused)
    }

    @Test
    fun `given another format, a quoted format or an unknown key, when the manifest is read, then each is refused`() {
        // Arrange
        val entry = entryText(name = "app")
        val manifests = listOf(
            // language=json
            """{"format":0,"version":"1","parts":[]}""",
            // language=json
            """{"format":-1,"version":"1","parts":[]}""",
            // language=json
            """{"format":2,"version":"1","parts":[]}""",
            // language=json
            """{"format":"1","version":"1","parts":[]}""",
            // language=json
            """{"format":1,"version":"1","parts":[],"extra":1}""",
            manifestText(entry.replace(oldValue = "}", newValue = /* language=json */ ""","extra":1}""")),
        )

        // Act
        val notRefused = manifests.filter { text ->
            runCatching { readManifest(text = text) }.exceptionOrNull() !is CatalogFileException
        }

        // Assert
        assertEquals(expected = emptyList(), actual = notRefused)
    }

    @Test
    fun `given an object with the same key twice, when the manifest is read, then each manifest is refused`() {
        // Arrange
        // language=json
        val twoNames = """{"name":"app","name":"home","file":"app.part.json","fingerprint":"$ANY_FINGERPRINT"}"""
        val manifests = listOf(
            // language=json
            """{"format":1,"version":"1","version":"2","parts":[]}""",
            manifestText(twoNames),
        )

        // Act
        val notRefused = manifests.filter { text ->
            runCatching { readManifest(text = text) }.exceptionOrNull() !is CatalogFileException
        }

        // Assert
        assertEquals(expected = emptyList(), actual = notRefused)
    }

    @Test
    fun `given bytes that are not valid UTF-8, when the manifest is read, then it is refused`() {
        // Arrange
        val bytes = manifestText(entryText(name = "app")).encodeToByteArray() + byteArrayOf(MALFORMED_BYTE)

        // Act
        val read = { CatalogManifestReader.read(bytes = bytes, sizeCap = SizeCap.DEFAULT) }

        // Assert
        val error = assertFailsWith<CatalogFileException> { read() }
        assertTrue(actual = error.message.orEmpty().contains(other = "not valid UTF-8"))
    }

    @Test
    fun `given a manifest not in canonical form, when it is read, then each is refused naming the manifest`() {
        // Arrange
        val canonical = CatalogManifestWriter.write(manifest = manifestWithEntries()).decodeToString()
        val nonCanonical = listOf(
            canonical.replace(oldValue = "\"format\": 1,\n", newValue = "\"format\": 1\n"),
            canonical.replace(oldValue = "\"format\": 1,", newValue = "\"format\":  1,"),
            "{\n  \"version\": \"1\",\n  \"format\": 1,\n  \"parts\": []\n}\n",
        )

        // Act
        val notRefused = nonCanonical.filterNot { text ->
            val error = runCatching { readManifest(text = text) }.exceptionOrNull()
            error is CatalogFileException && error.message.orEmpty().startsWith(prefix = "The manifest is not valid")
        }

        // Assert
        assertEquals(expected = emptyList(), actual = notRefused)
    }

    private companion object {
        const val MALFORMED_BYTE: Byte = -1
    }
}
