package dev.tonholo.ssrcompose.compiler.producer.scan

import dev.tonholo.ssrcompose.catalog.options.SourceFileList
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Paths
import kotlin.test.Test
import kotlin.test.assertEquals

class ExcludedSourcesTest {
    @TempDir
    lateinit var folder: File

    @Test
    fun `given no list, when read, then nothing is excluded`() {
        // Arrange
        val compilerRoots = listOf(File("/project/src/Compiled.kt"))

        // Act
        val excluded = ExcludedSources.read(listFile = null, compilerRoots = compilerRoots)

        // Assert
        assertEquals(expected = emptySet(), actual = excluded)
    }

    @Test
    fun `given a list the build has not written yet, when read, then nothing is excluded`() {
        // Arrange
        val missing = folder.resolve("missing.list")

        // Act
        val excluded = ExcludedSources.read(listFile = missing, compilerRoots = emptyList())

        // Assert
        assertEquals(expected = emptySet(), actual = excluded)
    }

    @Test
    fun `given a listed file the compiler receives, when read, then it is not excluded`() {
        // Arrange
        val list = folder.resolve("excluded.list")
        list.writeText(text = SourceFileList.encode(paths = listOf("/project/src/Compiled.kt", "/project/src/Left.kt")))
        val compilerRoots = listOf(File("/project/src/Compiled.kt"))

        // Act
        val excluded = ExcludedSources.read(listFile = list, compilerRoots = compilerRoots)

        // Assert
        assertEquals(expected = setOf(Paths.get("/project/src/Left.kt")), actual = excluded)
    }

    @Test
    fun `given a listed file under a compiler root folder, when read, then it is not excluded`() {
        // Arrange
        val list = folder.resolve("excluded.list")
        list.writeText(text = SourceFileList.encode(paths = listOf("/project/src/app/Compiled.kt")))
        val compilerRoots = listOf(File("/project/src"))

        // Act
        val excluded = ExcludedSources.read(listFile = list, compilerRoots = compilerRoots)

        // Assert
        assertEquals(expected = emptySet(), actual = excluded)
    }

    @Test
    fun `given a listed path with a parent step, when read, then the path is normalized`() {
        // Arrange
        val list = folder.resolve("excluded.list")
        list.writeText(text = SourceFileList.encode(paths = listOf("/project/src/../src/Excluded.kt")))

        // Act
        val excluded = ExcludedSources.read(listFile = list, compilerRoots = emptyList())

        // Assert
        assertEquals(expected = setOf(Paths.get("/project/src/Excluded.kt")), actual = excluded)
    }
}
