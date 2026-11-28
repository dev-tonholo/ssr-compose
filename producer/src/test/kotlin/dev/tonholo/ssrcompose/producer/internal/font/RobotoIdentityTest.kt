package dev.tonholo.ssrcompose.producer.internal.font

import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.platform.PlatformFont
import dev.tonholo.ssrcompose.producer.testing.sha256Hex
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RobotoIdentityTest {
    @Test
    fun `given the bundled Roboto, when its fonts are listed, then each identity is its own file's SHA-256`() {
        // Arrange
        val expected = RobotoFace.entries.map { face -> "sha256:${sha256Hex(data = face.readBytes())}" }

        // Act
        val fonts = assertIs<FontListFontFamily>(value = bundledRobotoFamily()).fonts

        // Assert
        assertEquals(expected = expected, actual = fonts.map { font -> assertIs<PlatformFont>(value = font).identity })
    }
}
