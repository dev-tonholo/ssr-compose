package dev.tonholo.ssrcompose.tooling.font

import dev.tonholo.ssrcompose.tooling.StepException
import dev.tonholo.ssrcompose.tooling.font.FontFixture.Companion.BOLD
import dev.tonholo.ssrcompose.tooling.font.FontFixture.Companion.REGULAR
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.RandomAccessFile
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** `ssrFontVerify` of contracts/build-tooling.md, section Fonts. */
class FontVerifyTest {
    @TempDir
    lateinit var root: File

    private fun synced(): ServerFontFixture = ServerFontFixture(root = root).apply {
        publish(version = VERSION, REGULAR, BOLD)
        sync(version = VERSION)
    }

    private fun verifyFailure(fixture: ServerFontFixture, versions: List<String> = listOf(VERSION)): Throwable? =
        runCatching { FontVerify.run(versions = versions, savedRoot = fixture.savedRoot, lockFile = fixture.lockFile) }
            .exceptionOrNull()

    private fun assertFailsNaming(failure: Throwable?, name: String) {
        assertEquals(expected = StepException::class, actual = failure?.let { it::class })
        assertContains(charSequence = failure?.message.orEmpty(), other = name)
    }

    private fun assertOnlyLinkReported(failure: Throwable?) {
        val message = failure?.message.orEmpty()
        assertContains(charSequence = message, other = "symbolic link")
        listOf("does not match its fingerprint", "is not valid", "is missing", "has no saved copy").forEach { problem ->
            assertFalse(actual = message.contains(problem), message = message)
        }
    }

    @Test
    fun `given a synced bundle, when verified, then it passes`() {
        // Arrange
        val fixture = synced()

        // Act
        val failure = verifyFailure(fixture = fixture)

        // Assert
        assertEquals(expected = null, actual = failure)
    }

    @Test
    fun `given a saved fonts json that does not match the lock, when verified, then it fails naming it`() {
        // Arrange
        val fixture = synced()
        val manifest = fixture.savedRoot.resolve("$VERSION/$FONTS_JSON")
        manifest.appendText("\n")

        // Act
        val failure = verifyFailure(fixture = fixture)

        // Assert
        assertFailsNaming(failure = failure, name = FONTS_JSON)
    }

    @Test
    fun `given a saved bundle with no entry in the lock, when verified, then it fails naming the version`() {
        // Arrange
        val fixture = synced()
        fixture.lockFile.writeText(/* language=json */ """{"fonts":[]}""")

        // Act
        val failure = verifyFailure(fixture = fixture)

        // Assert
        assertFailsNaming(failure = failure, name = "font version $VERSION")
    }

    @Test
    fun `given a saved font file that does not match its fingerprint, when verified, then it fails naming it`() {
        // Arrange
        val fixture = synced()
        fixture.savedRoot.resolve("$VERSION/$BOLD_FILE").writeText("tampered")

        // Act
        val failure = verifyFailure(fixture = fixture)

        // Assert
        assertFailsNaming(failure = failure, name = BOLD_FILE)
    }

    @Test
    fun `given a listed font file that is missing, when verified, then it fails naming it`() {
        // Arrange
        val fixture = synced()
        fixture.savedRoot.resolve("$VERSION/$BOLD_FILE").delete()

        // Act
        val failure = verifyFailure(fixture = fixture)

        // Assert
        assertFailsNaming(failure = failure, name = BOLD_FILE)
    }

    @Test
    fun `given a font file the fonts json does not list, when verified, then it fails naming it`() {
        // Arrange
        val fixture = synced()
        fixture.savedRoot.resolve("$VERSION/$UNLISTED_FILE").writeText("extra")

        // Act
        val failure = verifyFailure(fixture = fixture)

        // Assert
        assertFailsNaming(failure = failure, name = UNLISTED_FILE)
    }

    @Test
    fun `given a declared font bundle with no saved copy, when verified, then it fails naming the version`() {
        // Arrange
        val fixture = synced()

        // Act
        val failure = verifyFailure(fixture = fixture, versions = listOf(VERSION, MISSING_VERSION))

        // Assert
        assertFailsNaming(failure = failure, name = "font version $MISSING_VERSION")
    }

    @Test
    fun `given a font file that is a link outside the folder, when verified, then it fails naming it`() {
        // Arrange
        val fixture = synced()
        fixture.replaceWithLink(version = VERSION, relativePath = BOLD_FILE)

        // Act
        val failure = verifyFailure(fixture = fixture)

        // Assert
        assertFailsNaming(failure = failure, name = BOLD_FILE)
        assertContains(charSequence = failure?.message.orEmpty(), other = "symbolic link")
    }

    @Test
    fun `given a fonts json that is a link to changed bytes, when verified, then only the link is reported`() {
        // Arrange
        val fixture = synced()
        fixture.replaceWithLink(version = VERSION, relativePath = FONTS_JSON).writeText("not json")

        // Act
        val failure = verifyFailure(fixture = fixture)

        // Assert
        assertOnlyLinkReported(failure = failure)
    }

    @Test
    fun `given a font file that is a link to changed bytes, when verified, then only the link is reported`() {
        // Arrange
        val fixture = synced()
        fixture.replaceWithLink(version = VERSION, relativePath = BOLD_FILE).writeText("tampered")

        // Act
        val failure = verifyFailure(fixture = fixture)

        // Assert
        assertOnlyLinkReported(failure = failure)
    }

    @Test
    fun `given a family folder that is a link to changed files, when verified, then only the link is reported`() {
        // Arrange
        val fixture = synced()
        val linked = fixture.replaceFolderWithLink(version = VERSION, relativePath = FAMILY_FOLDER)
        linked.resolve(BOLD_FILE_NAME).writeText("tampered")

        // Act
        val failure = verifyFailure(fixture = fixture)

        // Assert
        assertOnlyLinkReported(failure = failure)
    }

    @Test
    fun `given a saved folder that is a link to changed files, when verified, then only the link is reported`() {
        // Arrange
        val fixture = synced()
        val linked = fixture.replaceFolderWithLink(version = VERSION, relativePath = WHOLE_FOLDER)
        linked.resolve(BOLD_FILE).writeText("tampered")

        // Act
        val failure = verifyFailure(fixture = fixture)

        // Assert
        assertOnlyLinkReported(failure = failure)
    }

    @Test
    fun `given a saved root that is a link to another folder, when verified, then only the link is reported`() {
        // Arrange
        val fixture = synced()
        fixture.replaceSavedRootWithLink()

        // Act
        val failure = verifyFailure(fixture = fixture)

        // Assert
        assertOnlyLinkReported(failure = failure)
    }

    @Test
    fun `given a saved fonts json of another version than its folder, when verified, then it fails naming both`() {
        // Arrange
        val fixture = synced()
        fixture.misfile(from = VERSION, to = MISFILED_VERSION)

        // Act
        val failure = verifyFailure(fixture = fixture, versions = listOf(MISFILED_VERSION))

        // Assert
        assertFailsNaming(failure = failure, name = "font version $MISFILED_VERSION is for font version $VERSION")
    }

    @Test
    fun `given a saved fonts json over the size cap, when verified, then it fails saying so`() {
        // Arrange
        val fixture = synced()
        val manifest = fixture.savedRoot.resolve("$VERSION/$FONTS_JSON")
        manifest.writeText(manifest.readText().padEnd(length = MANIFEST_SIZE_CAP_BYTES + 1, padChar = ' '))

        // Act
        val failure = verifyFailure(fixture = fixture)

        // Assert
        assertFailsNaming(failure = failure, name = "$FONTS_JSON of font version $VERSION is over its size cap")
    }

    @Test
    fun `given a saved font file over the size cap, when verified, then it fails saying so`() {
        // Arrange
        val fixture = synced()
        RandomAccessFile(fixture.savedRoot.resolve("$VERSION/$BOLD_FILE"), "rw").use { file ->
            file.setLength(FONT_FILE_SIZE_CAP_BYTES + 1)
        }

        // Act
        val failure = verifyFailure(fixture = fixture)

        // Assert
        assertFailsNaming(failure = failure, name = "$BOLD_FILE of font version $VERSION is over its size cap")
    }

    private companion object {
        const val MANIFEST_SIZE_CAP_BYTES = 1_048_576
        const val FONT_FILE_SIZE_CAP_BYTES = 16L * 1_048_576
        const val MISFILED_VERSION = "3"
        const val VERSION = "1"
        const val MISSING_VERSION = "2"
        const val FONTS_JSON = "fonts.json"
        const val WHOLE_FOLDER = ""
        const val FAMILY_FOLDER = "brand"
        const val BOLD_FILE_NAME = "700-normal.ttf"
        const val BOLD_FILE = "$FAMILY_FOLDER/$BOLD_FILE_NAME"
        const val UNLISTED_FILE = "brand/900-normal.ttf"
    }
}
