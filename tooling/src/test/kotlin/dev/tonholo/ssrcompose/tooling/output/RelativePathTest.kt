package dev.tonholo.ssrcompose.tooling.output

import kotlin.test.Test
import kotlin.test.assertEquals

/** Outputs are written by relative path only, so they hold no absolute path and never leave their folder. */
class RelativePathTest {
    @Test
    fun `given paths that are absolute or leave their folder, when a RelativePath is created, then each is rejected`() {
        // Arrange
        val paths = listOf("", "/fonts.json", "../fonts.json", "brand/../../x", "./fonts.json", "brand//a", "brand/",
            "brand\\a.ttf", "C:/fonts.json", "c:fonts.json")

        // Act
        val notRejected = paths.filter { path -> runCatching { RelativePath(value = path) }.isSuccess }

        // Assert
        assertEquals(expected = emptyList(), actual = notRejected)
    }

    @Test
    fun `given plain relative paths, when a RelativePath is created, then each is kept`() {
        // Arrange
        val paths = listOf("fonts.json", "brand/400-normal.ttf", "ssrcompose/fonts/1.0/fonts.json")

        // Act
        val actual = paths.map { path -> RelativePath(value = path).value }

        // Assert
        assertEquals(expected = paths, actual = actual)
    }
}
