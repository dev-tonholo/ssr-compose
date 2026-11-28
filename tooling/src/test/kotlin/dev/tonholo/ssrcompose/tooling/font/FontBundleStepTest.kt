package dev.tonholo.ssrcompose.tooling.font

import dev.tonholo.ssrcompose.core.fingerprint.Fingerprint
import dev.tonholo.ssrcompose.core.font.FontBundleManifest
import dev.tonholo.ssrcompose.core.font.FontBundleReader
import dev.tonholo.ssrcompose.core.font.FontBundleVersion
import dev.tonholo.ssrcompose.core.font.FontEntry
import dev.tonholo.ssrcompose.tooling.StepException
import dev.tonholo.ssrcompose.tooling.font.FontFixture.Companion.BOLD
import dev.tonholo.ssrcompose.tooling.font.FontFixture.Companion.FAMILY
import dev.tonholo.ssrcompose.tooling.font.FontFixture.Companion.REGULAR
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.RandomAccessFile
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/** `ssrFontBundle` of contracts/build-tooling.md, section Fonts. */
class FontBundleStepTest {
    @TempDir
    lateinit var first: File

    @TempDir
    lateinit var second: File

    private fun failureOf(fixture: FontFixture, version: String = VERSION, declarations: List<FontDeclaration>) =
        runCatching { fixture.bundle(version = version, declarations = declarations) }.exceptionOrNull()

    @Test
    fun `given declared fonts, when bundled, then fonts json lists each sorted and each file keeps its bytes`() {
        // Arrange
        val fixture = FontFixture(root = first)
        val bold = fixture.declare(weight = BOLD)
        val regular = fixture.declare(weight = REGULAR, extension = "OTF")

        // Act
        val bundle = fixture.bundle(version = VERSION, declarations = listOf(bold, regular))

        // Assert
        val expected = FontBundleManifest(
            version = FontBundleVersion(value = VERSION),
            fonts = listOf(
                entryOf(declaration = regular, file = REGULAR_FILE),
                entryOf(declaration = bold, file = BOLD_FILE),
            ),
        )
        val written = FontBundleReader.read(bytes = bundle.resolve(FONTS_JSON).readBytes())
        assertEquals(expected = expected, actual = written)
        assertContentEquals(expected = regular.file.readBytes(), actual = bundle.resolve(REGULAR_FILE).readBytes())
        assertContentEquals(expected = bold.file.readBytes(), actual = bundle.resolve(BOLD_FILE).readBytes())
    }

    @Test
    fun `given the same fonts in two folders and orders, when bundled, then the bundles have the same bytes`() {
        // Arrange
        val one = FontFixture(root = first)
        val other = FontFixture(root = second)
        val firstDeclarations = listOf(one.declare(weight = REGULAR), one.declare(weight = BOLD))
        val secondDeclarations = listOf(other.declare(weight = BOLD), other.declare(weight = REGULAR))

        // Act
        val firstBundle = one.bundle(version = VERSION, declarations = firstDeclarations)
        val secondBundle = other.bundle(version = VERSION, declarations = secondDeclarations)

        // Assert
        assertEquals(expected = filesOf(folder = firstBundle), actual = filesOf(folder = secondBundle))
        assertEquals(expected = false, actual = firstBundle.resolve(FONTS_JSON).readText().contains(first.absolutePath))
    }

    @Test
    fun `given an earlier bundle with another font, when bundled again, then only the declared fonts are left`() {
        // Arrange
        val fixture = FontFixture(root = first)
        fixture.bundle(version = VERSION, declarations = listOf(fixture.declare(weight = BOLD)))

        // Act
        val bundle = fixture.bundle(version = VERSION, declarations = listOf(fixture.declare(weight = REGULAR)))

        // Assert
        val expected = listOf("brand/400-normal.ttf", FONTS_JSON)
        assertEquals(expected = expected, actual = filesOf(folder = bundle).keys.toList())
    }

    @Test
    fun `given two declarations with the same family, weight and italic, when bundled, then the step fails`() {
        // Arrange
        val fixture = FontFixture(root = first)
        val declarations = listOf(fixture.declare(weight = REGULAR), fixture.declare(weight = REGULAR, text = "other"))

        // Act
        val failure = failureOf(fixture = fixture, declarations = declarations)

        // Assert
        assertEquals(expected = StepException::class, actual = failure?.let { it::class })
    }

    @Test
    fun `given a declared file that is not ttf or otf, when bundled, then the step fails naming it`() {
        // Arrange
        val fixture = FontFixture(root = first)
        val declaration = fixture.declare(weight = REGULAR, extension = "woff2")

        // Act
        val failure = failureOf(fixture = fixture, declarations = listOf(declaration))

        // Assert
        assertEquals(expected = StepException::class, actual = failure?.let { it::class })
        assertContains(charSequence = failure?.message.orEmpty(), other = declaration.file.name)
    }

    @Test
    fun `given a declared file over 16 MiB, when bundled, then the step fails`() {
        // Arrange
        val fixture = FontFixture(root = first)
        val declaration = fixture.declare(weight = REGULAR)
        RandomAccessFile(declaration.file, "rw").use { it.setLength(FONT_FILE_CAP + 1) }

        // Act
        val failure = failureOf(fixture = fixture, declarations = listOf(declaration))

        // Assert
        assertEquals(expected = StepException::class, actual = failure?.let { it::class })
    }

    @Test
    fun `given a declared file that does not exist, when bundled, then the step fails naming it`() {
        // Arrange
        val fixture = FontFixture(root = first)
        val declaration = fixture.declare(weight = REGULAR).also { it.file.delete() }

        // Act
        val failure = failureOf(fixture = fixture, declarations = listOf(declaration))

        // Assert
        assertEquals(expected = StepException::class, actual = failure?.let { it::class })
        assertContains(charSequence = failure?.message.orEmpty(), other = declaration.file.name)
    }

    @Test
    fun `given declared fonts whose fonts json would be over the reader's cap, when bundled, then the step fails`() {
        // Arrange
        val fixture = FontFixture(root = first)
        val declared = fixture.declare(weight = REGULAR)
        val declarations = List(size = OVER_CAP_FAMILIES) { index ->
            declared.copy(family = "f$index".padEnd(length = MAX_FAMILY_LENGTH, padChar = 'x'))
        }

        // Act
        val failure = failureOf(fixture = fixture, declarations = declarations)

        // Assert
        assertEquals(expected = StepException::class, actual = failure?.let { it::class })
    }

    @Test
    fun `given a bad font version or a bad family, when bundled, then the step fails`() {
        // Arrange
        val fixture = FontFixture(root = first)
        val badFamily = fixture.declare(weight = REGULAR).copy(family = "default")

        // Act
        val failures = listOf(
            failureOf(fixture = fixture, version = "../1", declarations = listOf(fixture.declare(weight = REGULAR))),
            failureOf(fixture = fixture, declarations = listOf(badFamily)),
        )

        // Assert
        val actual = failures.map { failure -> failure?.let { it::class } }
        assertEquals(expected = listOf(StepException::class, StepException::class), actual = actual)
    }

    private fun entryOf(declaration: FontDeclaration, file: String) = FontEntry(
        family = FAMILY,
        weight = declaration.weight,
        italic = declaration.italic,
        file = file,
        fingerprint = Fingerprint.of(bytes = declaration.file.readBytes()),
    )

    private fun filesOf(folder: File): Map<String, List<Byte>> = folder.walkTopDown()
        .filter { it.isFile }
        .associate { it.relativeTo(folder).invariantSeparatorsPath to it.readBytes().toList() }
        .toSortedMap()

    private companion object {
        const val VERSION = "1"
        const val FONTS_JSON = "fonts.json"
        const val REGULAR_FILE = "brand/400-normal.otf"
        const val BOLD_FILE = "brand/700-normal.ttf"
        const val FONT_FILE_CAP = 16L * 1_048_576
        const val MAX_FAMILY_LENGTH = 64
        const val OVER_CAP_FAMILIES = 5_000
    }
}
