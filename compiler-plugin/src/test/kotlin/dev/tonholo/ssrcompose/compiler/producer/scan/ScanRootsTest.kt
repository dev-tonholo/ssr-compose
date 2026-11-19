package dev.tonholo.ssrcompose.compiler.producer.scan

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class ScanRootsTest {
    @Test
    fun `given a compiler file inside a source folder, when the roots are chosen, then only the folder is kept`() {
        // Arrange
        val folder = File("/project/src/main/kotlin")
        val compilerFile = File("/project/src/main/kotlin/app/Screen.kt")

        // Act
        val roots = ScanRoots.of(sourceFolders = listOf(folder), compilerRoots = listOf(compilerFile))

        // Assert
        assertEquals(expected = listOf(folder), actual = roots)
    }

    @Test
    fun `given a compiler file outside every source folder, when the roots are chosen, then it is kept`() {
        // Arrange
        val folder = File("/project/src/main/kotlin")
        val compilerFile = File("/project/src/main/kotlin-extra/Screen.kt")

        // Act
        val roots = ScanRoots.of(sourceFolders = listOf(folder), compilerRoots = listOf(compilerFile))

        // Assert
        assertEquals(expected = listOf(folder, compilerFile), actual = roots)
    }

    @Test
    fun `given one folder written two ways, when the roots are chosen, then it is walked once`() {
        // Arrange
        val folder = File("/project/src/main/kotlin")
        val otherSpelling = File("/project/src/../src/main/./kotlin")
        val compilerFile = File("/project/src/main/../main/kotlin/app/Screen.kt")

        // Act
        val roots = ScanRoots.of(sourceFolders = listOf(folder, otherSpelling), compilerRoots = listOf(compilerFile))

        // Assert
        assertEquals(expected = listOf(folder), actual = roots)
    }

    @Test
    fun `given a relative folder, when the roots are chosen, then it is matched as an absolute path`() {
        // Arrange
        val relative = File("src/main/kotlin")
        val absolute = relative.absoluteFile

        // Act
        val roots = ScanRoots.of(sourceFolders = listOf(relative, absolute), compilerRoots = emptyList())

        // Assert
        assertEquals(expected = listOf(absolute), actual = roots)
    }
}
