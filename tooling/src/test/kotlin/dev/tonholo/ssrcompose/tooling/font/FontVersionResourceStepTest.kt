package dev.tonholo.ssrcompose.tooling.font

import dev.tonholo.ssrcompose.core.font.FontBundleVersion
import dev.tonholo.ssrcompose.core.font.FontVersionResource
import dev.tonholo.ssrcompose.tooling.StepException
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

/** `ssrFontVersion<Variant>`: the generated resource that carries the app's font version to the consumer. */
class FontVersionResourceStepTest {
    @TempDir
    lateinit var folder: File

    @Test
    fun `given a font version, when the resource is written, then it holds that version and nothing else is left`() {
        // Arrange
        folder.resolve("stale.txt").writeText("stale")

        // Act
        FontVersionResourceStep.run(version = VERSION, resourceFolder = folder)

        // Assert
        val written = FontVersionResource.read(bytes = folder.resolve(RESOURCE_PATH).readBytes())
        assertEquals(expected = FontBundleVersion(value = VERSION), actual = written)
        assertEquals(expected = listOf(RESOURCE_PATH), actual = filesOf(folder = folder))
    }

    @Test
    fun `given a bad font version, when the resource is written, then the step fails`() {
        // Arrange
        val version = "../1"

        // Act
        val failure = runCatching { FontVersionResourceStep.run(version = version, resourceFolder = folder) }

        // Assert
        assertEquals(expected = StepException::class, actual = failure.exceptionOrNull()?.let { it::class })
    }

    private fun filesOf(folder: File): List<String> =
        folder.walkTopDown().filter { it.isFile }.map { it.relativeTo(folder).invariantSeparatorsPath }.toList()

    private companion object {
        const val VERSION = "brand-2"
        const val RESOURCE_PATH = "ssrcompose/font-version"
    }
}
