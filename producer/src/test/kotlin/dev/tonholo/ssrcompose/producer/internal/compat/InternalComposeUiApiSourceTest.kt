package dev.tonholo.ssrcompose.producer.internal.compat

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val INTERNAL_API_MARKER = "InternalComposeUiApi"
private const val SOURCES_FOLDER = "src"
private const val BUILD_SCRIPT = "build.gradle.kts"
private const val MAIN_SOURCES_PREFIX = "src/main/"
private const val KOTLIN_SUFFIX = ".kt"
private const val COMPAT_FOLDER = "/dev/tonholo/ssrcompose/producer/internal/compat/"

class InternalComposeUiApiSourceTest {
    @Test
    fun `given the producer's sources, when searched, then only the compat package names Compose UI's internal API`() {
        // Arrange
        val sources = File(SOURCES_FOLDER).walkTopDown().filter { it.isFile } + File(BUILD_SCRIPT)

        // Act
        val outsideCompat = sources.map { it.invariantSeparatorsPath }.filterNot { COMPAT_FOLDER in it }.toList()
        val naming = outsideCompat.filter { path -> INTERNAL_API_MARKER in File(path).readText() }

        // Assert
        assertTrue(actual = outsideCompat.any { it.startsWith(MAIN_SOURCES_PREFIX) && it.endsWith(KOTLIN_SUFFIX) })
        assertEquals(expected = emptyList(), actual = naming)
    }
}
