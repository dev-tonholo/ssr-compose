package dev.tonholo.ssrcompose.core.font

import kotlin.test.Test
import kotlin.test.assertEquals

/** Building a manifest from fonts in any order, as the build does from the declared fonts. */
class FontBundleManifestTest {
    private val version = FontBundleVersion(value = "1")

    private fun entry(weight: Int, italic: Boolean) = FontEntry(
        family = BRAND,
        weight = weight,
        italic = italic,
        file = FontEntry.fileNameOf(family = BRAND, weight = weight, italic = italic, extension = "ttf"),
        fingerprint = FINGERPRINT,
    )

    @Test
    fun `given fonts in any order, when a manifest is built from them, then it lists them sorted`() {
        // Arrange
        val sorted = listOf(
            entry(weight = REGULAR, italic = false),
            entry(weight = REGULAR, italic = true),
            entry(weight = BOLD, italic = false),
        )

        // Act
        val actual = FontBundleManifest.sortedOf(version = version, fonts = sorted.reversed())

        // Assert
        assertEquals(expected = FontBundleManifest(version = version, fonts = sorted), actual = actual)
    }

    @Test
    fun `given a repeated font, when a manifest is built from them, then it is rejected`() {
        // Arrange
        val fonts = listOf(entry(weight = REGULAR, italic = false), entry(weight = REGULAR, italic = false))

        // Act
        val actual = runCatching { FontBundleManifest.sortedOf(version = version, fonts = fonts) }
            .exceptionOrNull()?.let { failure -> failure::class }

        // Assert
        assertEquals(expected = IllegalArgumentException::class, actual = actual)
    }

    private companion object {
        const val BRAND = "brand"
        const val REGULAR = 400
        const val BOLD = 700
        const val FINGERPRINT = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
    }
}
