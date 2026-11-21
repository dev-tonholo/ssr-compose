package dev.tonholo.ssrcompose.tooling.output

import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

/** Writing outputs by relative path, clearing a folder, and replacing a folder whole. */
class OutputFolderTest {
    @TempDir
    lateinit var root: File

    private fun filesUnder(folder: File): List<String> = folder.walkTopDown()
        .filter { it.isFile }
        .map { it.relativeTo(folder).invariantSeparatorsPath }
        .sorted()
        .toList()

    private fun outsideFileWithLinkFrom(link: File): File {
        val outsideFolder = root.resolve("outside").apply { mkdirs() }
        val outsideFile = outsideFolder.resolve("secret.txt").apply { writeText(text = CONTENT) }
        link.parentFile.mkdirs()
        Files.createSymbolicLink(link.toPath(), outsideFolder.toPath())
        return outsideFile
    }

    @Test
    fun `given a nested relative path, when bytes are written, then the file holds them under the root`() {
        // Arrange
        val folder = OutputFolder(root = root.resolve("out"))

        // Act
        folder.write(path = RelativePath(value = NESTED), bytes = CONTENT.encodeToByteArray())

        // Assert
        assertEquals(expected = CONTENT, actual = root.resolve("out/$NESTED").readText())
    }

    @Test
    fun `given a folder holding a link to a folder outside it, when it is cleared, then the outside file stays`() {
        // Arrange
        val outsideFile = outsideFileWithLinkFrom(link = root.resolve("out/link"))

        // Act
        OutputFolder(root = root.resolve("out")).clear()

        // Assert
        assertEquals(expected = true, actual = outsideFile.exists())
        assertEquals(expected = emptyList(), actual = root.resolve("out").list().orEmpty().toList())
    }

    @Test
    fun `given a folder holding a link to a folder outside it, when it is replaced, then the outside file stays`() {
        // Arrange
        val target = root.resolve("out")
        val outsideFile = outsideFileWithLinkFrom(link = target.resolve("link"))

        // Act
        OutputFolder.replace(target = target) { staged ->
            staged.write(path = RelativePath(value = NESTED), bytes = CONTENT.encodeToByteArray())
        }

        // Assert
        assertEquals(expected = true, actual = outsideFile.exists())
        assertEquals(expected = listOf(NESTED), actual = filesUnder(folder = target))
    }

    @Test
    fun `given a filling that fails, when a folder is replaced, then the old folder is kept as it was`() {
        // Arrange
        val target = root.resolve("saved").apply { mkdirs() }
        target.resolve("fonts.json").writeText(text = CONTENT)

        // Act
        runCatching {
            OutputFolder.replace(target = target) { staged ->
                staged.write(path = RelativePath(value = NESTED), bytes = CONTENT.encodeToByteArray())
                error("a fetch failed")
            }
        }

        // Assert
        assertEquals(expected = listOf("fonts.json"), actual = filesUnder(folder = target))
        assertEquals(expected = CONTENT, actual = target.resolve("fonts.json").readText())
        assertEquals(expected = listOf("saved"), actual = root.list().orEmpty().toList())
    }

    @Test
    fun `given a filling that succeeds, when a folder is replaced, then it holds only the new files`() {
        // Arrange
        val target = root.resolve("saved").apply { mkdirs() }
        target.resolve("stale.ttf").writeText(text = CONTENT)

        // Act
        OutputFolder.replace(target = target) { staged ->
            staged.write(path = RelativePath(value = NESTED), bytes = CONTENT.encodeToByteArray())
        }

        // Assert
        assertEquals(expected = listOf(NESTED), actual = filesUnder(folder = target))
        assertEquals(expected = listOf("saved"), actual = root.list().orEmpty().toList())
    }

    @Test
    fun `given siblings named like a version with a suffix, when a folder is replaced, then they are kept`() {
        // Arrange
        val target = root.resolve("1").apply { mkdirs() }
        val siblings = listOf("1.staging", "1.previous").map { name -> root.resolve(name).apply { mkdirs() } }
        siblings.forEach { sibling -> sibling.resolve("fonts.json").writeText(text = CONTENT) }

        // Act
        OutputFolder.replace(target = target) { staged ->
            staged.write(path = RelativePath(value = NESTED), bytes = CONTENT.encodeToByteArray())
        }

        // Assert
        assertEquals(expected = listOf("1", "1.previous", "1.staging"), actual = root.list().orEmpty().sorted())
        siblings.forEach { sibling ->
            assertEquals(expected = listOf("fonts.json"), actual = filesUnder(folder = sibling))
        }
    }

    private companion object {
        const val NESTED = "brand/400-normal.ttf"
        const val CONTENT = "bytes"
    }
}
