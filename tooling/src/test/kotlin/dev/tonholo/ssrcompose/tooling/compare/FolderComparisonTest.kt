package dev.tonholo.ssrcompose.tooling.compare

import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/** The byte for byte compare of a built folder with a saved one, naming each file that differs. */
class FolderComparisonTest {
    @TempDir
    lateinit var built: File

    @TempDir
    lateinit var saved: File

    private fun File.put(path: String, text: String) {
        resolve(path).apply { parentFile.mkdirs() }.writeText(text = text)
    }

    @Test
    fun `given equal folders, when compared, then nothing differs`() {
        // Arrange
        listOf(built, saved).forEach { folder ->
            folder.put(path = "fonts.json", text = "a")
            folder.put(path = "brand/400-normal.ttf", text = "b")
        }

        // Act
        val actual = FolderComparison.differingFiles(expected = built, actual = saved)

        // Assert
        assertEquals(expected = emptyList(), actual = actual)
    }

    @Test
    fun `given changed, missing and extra files, some empty, when compared, then each is named once, sorted`() {
        // Arrange
        built.put(path = "fonts.json", text = "a")
        built.put(path = "brand/400-normal.ttf", text = "b")
        built.put(path = "brand/700-normal.ttf", text = "same")
        saved.put(path = "fonts.json", text = "A")
        saved.put(path = "brand/700-normal.ttf", text = "same")
        saved.put(path = "brand/900-normal.ttf", text = "c")
        built.put(path = "empty-built.ttf", text = "")
        saved.put(path = "empty-saved.ttf", text = "")

        // Act
        val actual = FolderComparison.differingFiles(expected = built, actual = saved)

        // Assert
        val expected =
            listOf("brand/400-normal.ttf", "brand/900-normal.ttf", "empty-built.ttf", "empty-saved.ttf", "fonts.json")
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given files of the same size that differ in their last byte, when compared, then the file is named`() {
        // Arrange
        built.put(path = "brand/400-normal.ttf", text = "x".repeat(n = LARGER_THAN_A_BUFFER) + "a")
        saved.put(path = "brand/400-normal.ttf", text = "x".repeat(n = LARGER_THAN_A_BUFFER) + "b")

        // Act
        val actual = FolderComparison.differingFiles(expected = built, actual = saved)

        // Assert
        assertEquals(expected = listOf("brand/400-normal.ttf"), actual = actual)
    }

    private companion object {
        const val LARGER_THAN_A_BUFFER = 100_000
    }
}
