package dev.tonholo.ssrcompose.tooling.font

import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

/** Reading a saved file refuses a link at the file's own path. */
class LinkSafeFilesTest {
    @TempDir
    lateinit var folder: File

    @Test
    fun `given a regular file, when it is read without following links, then it gives its bytes`() {
        // Arrange
        val file = folder.resolve("font.ttf").apply { writeText(CONTENT) }

        // Act
        val actual = file.readBytesWithoutFollowingLinks()

        // Assert
        assertEquals(expected = CONTENT, actual = actual.decodeToString())
    }

    @Test
    fun `given a link to a file, when it is read without following links, then it is refused`() {
        // Arrange
        val target = folder.resolve("target.ttf").apply { writeText(CONTENT) }
        val link = folder.resolve("link.ttf")
        Files.createSymbolicLink(link.toPath(), target.toPath())

        // Act
        val failure = runCatching { link.readBytesWithoutFollowingLinks() }.exceptionOrNull()

        // Assert
        assertEquals(expected = true, actual = failure is IOException, message = failure.toString())
    }

    private companion object {
        const val CONTENT = "font bytes"
    }
}
