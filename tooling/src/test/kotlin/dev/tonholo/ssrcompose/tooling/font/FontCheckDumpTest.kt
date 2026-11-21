package dev.tonholo.ssrcompose.tooling.font

import dev.tonholo.ssrcompose.tooling.StepException
import dev.tonholo.ssrcompose.tooling.font.FontFixture.Companion.BOLD
import dev.tonholo.ssrcompose.tooling.font.FontFixture.Companion.REGULAR
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

/** `ssrFontCheck` and `ssrFontDump` of contracts/build-tooling.md, section Fonts. */
class FontCheckDumpTest {
    @TempDir
    lateinit var root: File

    private fun checkFailure(fixture: FontFixture, bundle: File, version: String = VERSION): Throwable? =
        runCatching { FontCheck.run(bundleFolder = bundle, savedRoot = fixture.savedRoot, version = version) }
            .exceptionOrNull()

    @Test
    fun `given a bundle equal to the saved one, when checked, then the check passes`() {
        // Arrange
        val fixture = FontFixture(root = root)
        val declarations = listOf(fixture.declare(weight = REGULAR))
        fixture.publish(version = VERSION, declarations = declarations)
        val bundle = fixture.bundle(version = VERSION, declarations = declarations)

        // Act
        val failure = checkFailure(fixture = fixture, bundle = bundle)

        // Assert
        assertEquals(expected = null, actual = failure)
    }

    @Test
    fun `given no saved bundle for the version, when checked, then it says to dump, not to raise the version`() {
        // Arrange
        val fixture = FontFixture(root = root)
        val bundle = fixture.bundle(version = VERSION, declarations = listOf(fixture.declare(weight = REGULAR)))

        // Act
        val failure = checkFailure(fixture = fixture, bundle = bundle)

        // Assert
        assertEquals(expected = StepException::class, actual = failure?.let { it::class })
        assertContains(charSequence = failure?.message.orEmpty(), other = DUMP_TASK)
        assertEquals(expected = false, actual = failure?.message.orEmpty().contains(RAISE_ADVICE))
    }

    @Test
    fun `given a changed font with the same version, when checked, then it fails naming each differing file`() {
        // Arrange
        val fixture = FontFixture(root = root)
        fixture.publish(version = VERSION, declarations = listOf(fixture.declare(weight = REGULAR)))
        val changed = listOf(fixture.declare(weight = REGULAR, text = "changed"), fixture.declare(weight = BOLD))
        val bundle = fixture.bundle(version = VERSION, declarations = changed)

        // Act
        val failure = checkFailure(fixture = fixture, bundle = bundle)

        // Assert
        assertEquals(expected = StepException::class, actual = failure?.let { it::class })
        val message = failure?.message.orEmpty()
        listOf("brand/400-normal.ttf", "brand/700-normal.ttf", "fonts.json", DUMP_TASK).forEach { name ->
            assertContains(charSequence = message, other = name)
        }
    }

    @Test
    fun `given a saved bundle with a stale font file, when dumped, then it holds exactly the new bundle`() {
        // Arrange
        val fixture = FontFixture(root = root)
        val saved = fixture.publish(version = VERSION, declarations = listOf(fixture.declare(weight = BOLD)))
        val bundle = fixture.bundle(version = VERSION, declarations = listOf(fixture.declare(weight = REGULAR)))

        // Act
        FontDump.run(bundleFolder = bundle, savedRoot = fixture.savedRoot, version = VERSION)

        // Assert
        assertEquals(expected = listOf("brand/400-normal.ttf", "fonts.json"), actual = filesOf(folder = saved))
        val savedManifest = saved.resolve("fonts.json").readBytes()
        assertContentEquals(expected = bundle.resolve("fonts.json").readBytes(), actual = savedManifest)
    }

    @Test
    fun `given a built folder that is missing or has no fonts json, when dumped, then the saved bundle is kept`() {
        // Arrange
        val fixture = FontFixture(root = root)
        val saved = fixture.publish(version = VERSION, declarations = listOf(fixture.declare(weight = REGULAR)))
        val before = contentsOf(folder = saved)
        val withoutManifest = fixture.bundle(version = VERSION, declarations = listOf(fixture.declare(weight = BOLD)))
            .also { bundle -> bundle.resolve("fonts.json").delete() }
        val missing = root.resolve("build/missing")

        // Act
        val failures = listOf(missing, withoutManifest).map { bundle ->
            runCatching { FontDump.run(bundleFolder = bundle, savedRoot = fixture.savedRoot, version = VERSION) }
                .exceptionOrNull()
        }

        // Assert
        val actual = failures.map { failure -> failure?.let { it::class } }
        assertEquals(expected = listOf(StepException::class, StepException::class), actual = actual)
        assertEquals(expected = before, actual = contentsOf(folder = saved))
    }

    @Test
    fun `given saved bundles of two versions, when one is dumped, then the other is kept`() {
        // Arrange
        val fixture = FontFixture(root = root)
        val kept = fixture.publish(version = OTHER_VERSION, declarations = listOf(fixture.declare(weight = BOLD)))
        val before = filesOf(folder = kept)

        // Act
        fixture.publish(version = VERSION, declarations = listOf(fixture.declare(weight = REGULAR)))

        // Assert
        assertEquals(expected = before, actual = filesOf(folder = kept))
    }

    private fun contentsOf(folder: File): Map<String, List<Byte>> = folder.walkTopDown()
        .filter { it.isFile }
        .associate { it.relativeTo(folder).invariantSeparatorsPath to it.readBytes().toList() }

    private fun filesOf(folder: File): List<String> = folder.walkTopDown()
        .filter { it.isFile }
        .map { it.relativeTo(folder).invariantSeparatorsPath }
        .sorted()
        .toList()

    private companion object {
        const val VERSION = "2"
        const val OTHER_VERSION = "1"
        const val DUMP_TASK = "ssrFontDump"
        const val RAISE_ADVICE = "new font version"
    }
}
