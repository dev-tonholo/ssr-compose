package dev.tonholo.ssrcompose.core.font

import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class FontBundleReaderTest {
    private fun entry(
        family: String = BRAND,
        weight: Int = REGULAR,
        italic: Boolean = false,
        file: String = "$family/$weight-${if (italic) "italic" else "normal"}.ttf",
        fingerprint: String = FINGERPRINT_A,
        weightElement: JsonElement = JsonPrimitive(value = weight),
        italicElement: JsonElement = JsonPrimitive(value = italic),
    ): JsonObject = buildJsonObject {
        put(key = "family", value = family)
        put(key = "weight", element = weightElement)
        put(key = "italic", element = italicElement)
        put(key = "file", value = file)
        put(key = "fingerprint", value = fingerprint)
    }

    private fun manifest(
        vararg entries: JsonObject,
        format: Int = FORMAT,
        formatElement: JsonElement = JsonPrimitive(value = format),
    ): ByteArray = buildJsonObject {
        put(key = "format", element = formatElement)
        put(key = "version", value = "1")
        put(key = "fonts", element = buildJsonArray { entries.forEach { add(element = it) } })
    }.toString().encodeToByteArray()

    private fun failuresOf(manifests: List<ByteArray>): List<KClass<out Throwable>?> = manifests.map { bytes ->
        runCatching { FontBundleReader.read(bytes = bytes) }.exceptionOrNull()?.let { failure -> failure::class }
    }

    private fun allRefused(manifests: List<ByteArray>) = manifests.map { FontBundleException::class }

    private fun padded(bytes: ByteArray, toSize: Int): ByteArray =
        bytes + ByteArray(size = toSize - bytes.size) { SPACE }

    @Test
    fun `given the example of contracts font-bundle md, when it is read, then it lists both brand files`() {
        // Arrange
        val bytes = EXAMPLE.encodeToByteArray()
        val expected = FontBundleManifest(
            version = FontBundleVersion(value = "1"),
            fonts = listOf(
                FontEntry(
                    family = BRAND,
                    weight = REGULAR,
                    italic = false,
                    file = "brand/400-normal.ttf",
                    fingerprint = FINGERPRINT_A,
                ),
                FontEntry(
                    family = BRAND,
                    weight = BOLD,
                    italic = false,
                    file = "brand/700-normal.ttf",
                    fingerprint = FINGERPRINT_B,
                ),
            ),
        )

        // Act
        val actual = FontBundleReader.read(bytes = bytes)

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a file name not built from its family, weight and style, when read, then the bundle is rejected`() {
        // Arrange
        val manifests = listOf(
            "brand/../400-normal.ttf",
            "../brand/400-normal.ttf",
            "other/400-normal.ttf",
            "brand/700-normal.ttf",
            "brand/400-italic.ttf",
            "brand/400-normal.woff",
            "https://example.com/brand/400-normal.ttf",
            "/brand/400-normal.ttf",
            "brand\\400-normal.ttf",
        ).map { file -> manifest(entry(file = file)) }

        // Act
        val actual = failuresOf(manifests = manifests)

        // Assert
        assertEquals(expected = allRefused(manifests = manifests), actual = actual)
    }

    @Test
    fun `given two entries with the same family, weight and style, when read, then the bundle is rejected`() {
        // Arrange
        val manifests = listOf(manifest(entry(), entry(fingerprint = FINGERPRINT_B)))

        // Act
        val actual = failuresOf(manifests = manifests)

        // Assert
        assertEquals(expected = allRefused(manifests = manifests), actual = actual)
    }

    @Test
    fun `given a fonts json over 1 MiB, when read, then it is refused before parsing`() {
        // Arrange
        val manifests = listOf(padded(bytes = manifest(entry()), toSize = MANIFEST_CAP + 1))

        // Act
        val actual = failuresOf(manifests = manifests)

        // Assert
        assertEquals(expected = allRefused(manifests = manifests), actual = actual)
    }

    @Test
    fun `given malformed JSON over 1 MiB, when read, then the size limit is the reason`() {
        // Arrange
        val bytes = padded(bytes = "{".encodeToByteArray(), toSize = MANIFEST_CAP + 1)

        // Act
        val actual = runCatching { FontBundleReader.read(bytes = bytes) }.exceptionOrNull()?.message

        // Assert
        assertEquals(expected = "The fonts.json is larger than $MANIFEST_CAP bytes", actual = actual)
    }

    @Test
    fun `given a fonts json of exactly 1 MiB, when read, then it is read`() {
        // Arrange
        val manifests = listOf(padded(bytes = manifest(entry()), toSize = MANIFEST_CAP))

        // Act
        val actual = failuresOf(manifests = manifests)

        // Assert
        assertEquals(expected = listOf(null), actual = actual)
    }

    @Test
    fun `given a font file over 16 MiB, when its size is checked, then it is refused`() {
        // Arrange
        val sizeBytes = FONT_FILE_CAP + 1

        // Act
        val actual = runCatching { FontBundleReader.requireFontFileSize(sizeBytes = sizeBytes) }
            .exceptionOrNull()?.let { failure -> failure::class }

        // Assert
        assertEquals(expected = FontBundleException::class, actual = actual)
    }

    @Test
    fun `given a font file of exactly 16 MiB, when its size is checked, then it is accepted`() {
        // Arrange
        val sizeBytes = FONT_FILE_CAP

        // Act
        val actual = runCatching { FontBundleReader.requireFontFileSize(sizeBytes = sizeBytes) }.exceptionOrNull()

        // Assert
        assertEquals(expected = null, actual = actual)
    }

    @Test
    fun `given a format the reader does not know, when read, then the bundle is rejected`() {
        // Arrange
        val manifests = listOf(manifest(entry(), format = FORMAT + 1), manifest(entry(), format = FORMAT - 1))

        // Act
        val actual = failuresOf(manifests = manifests)

        // Assert
        assertEquals(expected = allRefused(manifests = manifests), actual = actual)
    }

    @Test
    fun `given a family that is not a plain identifier or is default, when read, then the bundle is rejected`() {
        // Arrange
        val manifests = listOf("brand-x", "1brand", "default", "b".repeat(n = MAX_FAMILY_LENGTH + 1), "class")
            .map { family -> manifest(entry(family = family)) }

        // Act
        val actual = failuresOf(manifests = manifests)

        // Assert
        assertEquals(expected = allRefused(manifests = manifests), actual = actual)
    }

    @Test
    fun `given a family that is a Windows device name in any case, when read, then the bundle is rejected`() {
        // Arrange
        val manifests = listOf("con", "CON", "Aux", "nul", "com1", "LPT9").map { family ->
            manifest(entry(family = family))
        }

        // Act
        val actual = failuresOf(manifests = manifests)

        // Assert
        assertEquals(expected = allRefused(manifests = manifests), actual = actual)
    }

    @Test
    fun `given two families that differ only by case, when read, then the bundle is rejected`() {
        // Arrange
        val manifests = listOf(manifest(entry(family = "Brand"), entry(family = BRAND)))

        // Act
        val actual = failuresOf(manifests = manifests)

        // Assert
        assertEquals(expected = allRefused(manifests = manifests), actual = actual)
    }

    @Test
    fun `given a weight outside 1 to 1,000, when read, then the bundle is rejected`() {
        // Arrange
        val manifests = listOf(MIN_WEIGHT - 1, MAX_WEIGHT + 1).map { weight -> manifest(entry(weight = weight)) }

        // Act
        val actual = failuresOf(manifests = manifests)

        // Assert
        assertEquals(expected = allRefused(manifests = manifests), actual = actual)
    }

    @Test
    fun `given a fingerprint that is not 64 lowercase hexadecimal digits, when read, then the bundle is rejected`() {
        // Arrange
        val fingerprints = listOf(
            "A".repeat(n = FINGERPRINT_LENGTH),
            "a".repeat(n = FINGERPRINT_LENGTH - 1),
            "g".repeat(n = FINGERPRINT_LENGTH),
        )
        val manifests = fingerprints.map { fingerprint -> manifest(entry(fingerprint = fingerprint)) }

        // Act
        val actual = failuresOf(manifests = manifests)

        // Assert
        assertEquals(expected = allRefused(manifests = manifests), actual = actual)
    }

    @Test
    fun `given entries not sorted by family, weight and style, when read, then the bundle is rejected`() {
        // Arrange
        val manifests = listOf(
            manifest(entry(weight = BOLD), entry(weight = REGULAR)),
            manifest(entry(italic = true), entry(italic = false)),
            manifest(entry(family = "zeta"), entry(family = BRAND)),
        )

        // Act
        val actual = failuresOf(manifests = manifests)

        // Assert
        assertEquals(expected = allRefused(manifests = manifests), actual = actual)
    }

    @Test
    fun `given limits at their edge and an otf file, when read, then the bundle is read`() {
        // Arrange
        val manifests = listOf(
            manifest(entry(weight = MIN_WEIGHT), entry(weight = MAX_WEIGHT)),
            manifest(entry(family = "b".repeat(n = MAX_FAMILY_LENGTH))),
            manifest(entry(file = "brand/400-normal.otf")),
            manifest(entry(italic = false), entry(italic = true)),
            manifest(entry(family = "com10"), entry(family = "console")),
            manifest(entry(family = "Brand"), entry(family = "other")),
        )

        // Act
        val actual = failuresOf(manifests = manifests)

        // Assert
        assertEquals(expected = manifests.map { null }, actual = actual)
    }

    @Test
    fun `given a format, weight or italic written as a string, when read, then the bundle is rejected`() {
        // Arrange
        val quotedFormat = manifest(entry(), formatElement = JsonPrimitive(value = FORMAT.toString()))
        val quotedWeight = manifest(entry(weightElement = JsonPrimitive(value = REGULAR.toString())))
        val quotedItalic = manifest(entry(italicElement = JsonPrimitive(value = false.toString())))
        val manifests = listOf(quotedFormat, quotedWeight, quotedItalic)

        // Act
        val actual = failuresOf(manifests = manifests)

        // Assert
        assertEquals(expected = allRefused(manifests = manifests), actual = actual)
    }

    @Test
    fun `given bytes that are not a fonts json, when read, then only a font bundle error is thrown`() {
        // Arrange
        val manifests = listOf(
            ByteArray(size = 0),
            "[1,2]".encodeToByteArray(),
            buildJsonObject { put(key = "format", element = JsonPrimitive(value = "one")) }
                .toString()
                .encodeToByteArray(),
        )

        // Act
        val actual = failuresOf(manifests = manifests)

        // Assert
        assertEquals(expected = allRefused(manifests = manifests), actual = actual)
    }

    private companion object {
        const val FORMAT = 1
        const val BRAND = "brand"
        const val REGULAR = 400
        const val BOLD = 700
        const val MIN_WEIGHT = 1
        const val MAX_WEIGHT = 1_000
        const val MAX_FAMILY_LENGTH = 64
        const val FINGERPRINT_LENGTH = 64
        const val MANIFEST_CAP = 1_048_576
        const val FONT_FILE_CAP = 16_777_216L
        const val SPACE = 0x20.toByte()
        const val FINGERPRINT_A = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        const val FINGERPRINT_B = "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210"

        // language=json
        const val EXAMPLE = """{
  "format": 1,
  "version": "1",
  "fonts": [
    {"family": "brand", "weight": 400, "italic": false, "file": "brand/400-normal.ttf", "fingerprint": "$FINGERPRINT_A"},
    {"family": "brand", "weight": 700, "italic": false, "file": "brand/700-normal.ttf", "fingerprint": "$FINGERPRINT_B"}
  ]
}"""
    }
}
