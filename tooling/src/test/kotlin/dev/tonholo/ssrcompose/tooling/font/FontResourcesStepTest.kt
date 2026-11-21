package dev.tonholo.ssrcompose.tooling.font

import dev.tonholo.ssrcompose.core.font.FontBundleVersion
import dev.tonholo.ssrcompose.core.font.FontVersionsIndex
import dev.tonholo.ssrcompose.tooling.StepException
import dev.tonholo.ssrcompose.tooling.font.FontFixture.Companion.BOLD
import dev.tonholo.ssrcompose.tooling.font.FontFixture.Companion.REGULAR
import dev.tonholo.ssrcompose.tooling.font.ServerFontFixture.Companion.filesOf
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

/** `ssrFontResources` of contracts/build-tooling.md, section Fonts. */
class FontResourcesStepTest {
    @TempDir
    lateinit var root: File

    private val output: File
        get() = root.resolve("server/build/ssr/font-resources")

    private fun synced(vararg versions: String): ServerFontFixture = ServerFontFixture(root = root).apply {
        versions.forEach { version ->
            publish(version = version, REGULAR, BOLD, text = "font $version")
            sync(version = version)
        }
    }

    private fun run(fixture: ServerFontFixture, versions: List<String>) =
        FontResourcesStep.run(
            versions = versions,
            savedRoot = fixture.savedRoot,
            lockFile = fixture.lockFile,
            outputFolder = output,
        )

    private fun failureOf(fixture: ServerFontFixture, versions: List<String> = listOf(FIRST)): Throwable? =
        runCatching { run(fixture = fixture, versions = versions) }.exceptionOrNull()

    private fun assertRefusedNaming(failure: Throwable?, name: String) {
        assertEquals(expected = StepException::class, actual = failure?.let { it::class })
        assertContains(charSequence = failure?.message.orEmpty(), other = name)
        assertEquals(expected = false, actual = output.resolve("ssrcompose/fonts/$FIRST").exists())
    }

    @Test
    fun `given saved bundles, when copied, then each keeps its bytes and the index lists the versions`() {
        // Arrange
        val fixture = synced(FIRST, SECOND)

        // Act
        run(fixture = fixture, versions = listOf(SECOND, FIRST))

        // Assert
        listOf(FIRST, SECOND).forEach { version ->
            val copied = filesOf(folder = output.resolve("ssrcompose/fonts/$version"))
            assertEquals(expected = fixture.savedFiles(version = version), actual = copied)
        }
        val index = FontVersionsIndex.read(bytes = output.resolve(INDEX_PATH).readBytes())
        assertEquals(expected = listOf(FIRST, SECOND).map { FontBundleVersion(value = it) }, actual = index)
    }

    @Test
    fun `given a saved folder with a file fonts json does not list, when copied, then that file is left out`() {
        // Arrange
        val fixture = synced(FIRST)
        fixture.savedRoot.resolve("$FIRST/$UNLISTED_FILE").writeText("extra")

        // Act
        run(fixture = fixture, versions = listOf(FIRST))

        // Assert
        assertEquals(expected = false, actual = output.resolve("ssrcompose/fonts/$FIRST/$UNLISTED_FILE").exists())
    }

    @Test
    fun `given an earlier output with a version no longer declared, when copied, then that version is gone`() {
        // Arrange
        val fixture = synced(FIRST, SECOND)
        run(fixture = fixture, versions = listOf(FIRST, SECOND))

        // Act
        run(fixture = fixture, versions = listOf(SECOND))

        // Assert
        assertEquals(expected = listOf(SECOND), actual = output.resolve("ssrcompose/fonts").list().orEmpty().toList())
    }

    @Test
    fun `given an earlier output and no declared version, when copied, then the output is left empty`() {
        // Arrange
        val fixture = synced(FIRST)
        run(fixture = fixture, versions = listOf(FIRST))

        // Act
        run(fixture = fixture, versions = emptyList())

        // Assert
        assertEquals(expected = emptyList(), actual = output.list().orEmpty().toList())
    }

    @Test
    fun `given a version declared twice and a bad font file, when copied, then the problem is reported once`() {
        // Arrange
        val fixture = synced(FIRST)
        fixture.savedRoot.resolve("$FIRST/$BOLD_FILE").writeText("tampered")

        // Act
        val failure = failureOf(fixture = fixture, versions = listOf(FIRST, FIRST))

        // Assert
        val reports = failure?.message.orEmpty().lines().count { line -> line.contains(BOLD_FILE) }
        assertEquals(expected = 1, actual = reports)
    }

    @Test
    fun `given a declared version with no saved copy, when copied, then the step fails`() {
        // Arrange
        val fixture = synced(FIRST)

        // Act
        val failure = runCatching { run(fixture = fixture, versions = listOf(FIRST, SECOND)) }.exceptionOrNull()

        // Assert
        assertEquals(expected = StepException::class, actual = failure?.let { it::class })
    }

    @Test
    fun `given a font file that is a link outside the folder, when copied, then the step refuses it`() {
        // Arrange
        val fixture = synced(FIRST)
        fixture.replaceWithLink(version = FIRST, relativePath = BOLD_FILE)

        // Act
        val failure = failureOf(fixture = fixture)

        // Assert
        assertRefusedNaming(failure = failure, name = BOLD_FILE)
    }

    @Test
    fun `given a saved root that is a link to another folder, when copied, then the step refuses it`() {
        // Arrange
        val fixture = synced(FIRST)
        fixture.replaceSavedRootWithLink()

        // Act
        val failure = failureOf(fixture = fixture)

        // Assert
        assertRefusedNaming(failure = failure, name = "symbolic link")
    }

    @Test
    fun `given a font file that does not match its fingerprint, when copied, then the step refuses it`() {
        // Arrange
        val fixture = synced(FIRST)
        fixture.savedRoot.resolve("$FIRST/$BOLD_FILE").writeText("tampered")

        // Act
        val failure = failureOf(fixture = fixture)

        // Assert
        assertRefusedNaming(failure = failure, name = BOLD_FILE)
    }

    @Test
    fun `given a fonts json that does not match the lock, when copied, then the step refuses it`() {
        // Arrange
        val fixture = synced(FIRST)
        val manifest = fixture.savedRoot.resolve("$FIRST/fonts.json")
        manifest.appendText("\n")

        // Act
        val failure = failureOf(fixture = fixture)

        // Assert
        assertRefusedNaming(failure = failure, name = "fonts.json of font version $FIRST")
    }

    @Test
    fun `given a fonts json of another version than its folder, when copied, then the step refuses it`() {
        // Arrange
        val fixture = synced(FIRST)
        fixture.misfile(from = FIRST, to = SECOND)

        // Act
        val failure = failureOf(fixture = fixture, versions = listOf(SECOND))

        // Assert
        assertEquals(expected = StepException::class, actual = failure?.let { it::class })
        assertContains(charSequence = failure?.message.orEmpty(), other = "is for font version $FIRST")
    }

    @Test
    fun `given a fonts json over the size cap, when copied, then the step refuses it saying so`() {
        // Arrange
        val fixture = synced(FIRST)
        val manifest = fixture.savedRoot.resolve("$FIRST/fonts.json")
        manifest.writeText(manifest.readText().padEnd(length = MANIFEST_SIZE_CAP_BYTES + 1, padChar = ' '))

        // Act
        val failure = failureOf(fixture = fixture)

        // Assert
        assertRefusedNaming(failure = failure, name = "fonts.json of font version $FIRST is over its size cap")
    }

    private companion object {
        const val MANIFEST_SIZE_CAP_BYTES = 1_048_576
        const val INDEX_PATH = "ssrcompose/font-versions.json"
        const val BOLD_FILE = "brand/700-normal.ttf"
        const val FIRST = "1"
        const val SECOND = "2"
        const val UNLISTED_FILE = "brand/900-normal.ttf"
    }
}
