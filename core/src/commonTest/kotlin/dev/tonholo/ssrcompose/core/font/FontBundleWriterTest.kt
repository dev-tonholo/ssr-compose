package dev.tonholo.ssrcompose.core.font

import kotlin.test.Test
import kotlin.test.assertEquals

/** The one written form of `fonts.json`, which the reader and the build's byte compare rely on. */
class FontBundleWriterTest {
    private fun entry(
        file: String,
        fingerprint: String,
        family: String = BRAND,
        weight: Int = REGULAR,
        italic: Boolean = false,
    ) = FontEntry(family = family, weight = weight, italic = italic, file = file, fingerprint = fingerprint)

    private val manifest = FontBundleManifest(
        version = FontBundleVersion(value = "1"),
        fonts = listOf(
            entry(weight = REGULAR, italic = false, file = REGULAR_FILE, fingerprint = FINGERPRINT_A),
            entry(weight = BOLD, italic = true, file = BOLD_ITALIC_FILE, fingerprint = FINGERPRINT_B),
        ),
    )

    @Test
    fun `given a manifest, when it is written, then the bytes are the golden form`() {
        // Arrange
        val expected = GOLDEN

        // Act
        val actual = FontBundleWriter.write(manifest = manifest).decodeToString()

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a written manifest, when it is read, then it is the same manifest`() {
        // Arrange
        val bytes = FontBundleWriter.write(manifest = manifest)

        // Act
        val actual = FontBundleReader.read(bytes = bytes)

        // Assert
        assertEquals(expected = manifest, actual = actual)
    }

    @Test
    fun `given a manifest whose bytes are over the reader's cap, when it is written, then it is refused`() {
        // Arrange
        val fonts = List(size = OVER_CAP_FAMILIES) { index ->
            val family = "family" + index.toString().padStart(length = FAMILY_INDEX_DIGITS, padChar = '0')
            entry(family = family, file = "$family/$REGULAR-normal.ttf", fingerprint = FINGERPRINT_A)
        }
        val tooLarge = FontBundleManifest(version = FontBundleVersion(value = "1"), fonts = fonts)

        // Act
        val failure = runCatching { FontBundleWriter.write(manifest = tooLarge) }.exceptionOrNull()

        // Assert
        assertEquals(expected = FontBundleException::class, actual = failure?.let { it::class })
    }

    private companion object {
        const val OVER_CAP_FAMILIES = 10_000
        const val FAMILY_INDEX_DIGITS = 5
        const val BRAND = "brand"
        const val REGULAR = 400
        const val BOLD = 700
        const val REGULAR_FILE = "brand/400-normal.ttf"
        const val BOLD_ITALIC_FILE = "brand/700-italic.ttf"
        const val FINGERPRINT_A = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        const val FINGERPRINT_B = "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210"

        // language=json
        const val GOLDEN = """{
  "format": 1,
  "version": "1",
  "fonts": [
    {
      "family": "brand",
      "weight": 400,
      "italic": false,
      "file": "brand/400-normal.ttf",
      "fingerprint": "$FINGERPRINT_A"
    },
    {
      "family": "brand",
      "weight": 700,
      "italic": true,
      "file": "brand/700-italic.ttf",
      "fingerprint": "$FINGERPRINT_B"
    }
  ]
}
"""
    }
}
