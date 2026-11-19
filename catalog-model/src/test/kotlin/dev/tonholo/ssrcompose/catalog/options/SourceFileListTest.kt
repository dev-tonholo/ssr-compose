package dev.tonholo.ssrcompose.catalog.options

import kotlin.test.Test
import kotlin.test.assertEquals

class SourceFileListTest {
    @Test
    fun `given paths with spaces and line breaks, when written and read, then each path comes back whole`() {
        // Arrange
        val paths = listOf("/project/src/My Screen.kt", "/project/src/line\nbreak.kt")

        // Act
        val decoded = SourceFileList.decode(text = SourceFileList.encode(paths = paths))

        // Assert
        assertEquals(expected = paths, actual = decoded)
    }

    @Test
    fun `given no paths, when written and read, then the list is empty`() {
        // Arrange
        val paths = emptyList<String>()

        // Act
        val decoded = SourceFileList.decode(text = SourceFileList.encode(paths = paths))

        // Assert
        assertEquals(expected = paths, actual = decoded)
    }
}
