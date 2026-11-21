package dev.tonholo.ssrcompose.tooling.font

import dev.tonholo.ssrcompose.catalog.bundle.fingerprint.Fingerprint
import dev.tonholo.ssrcompose.tooling.StepException
import dev.tonholo.ssrcompose.tooling.font.FontFixture.Companion.BOLD
import dev.tonholo.ssrcompose.tooling.font.FontFixture.Companion.REGULAR
import dev.tonholo.ssrcompose.tooling.font.ServerFontFixture.Companion.filesOf
import dev.tonholo.ssrcompose.tooling.lock.LockEntry
import dev.tonholo.ssrcompose.tooling.lock.LockFile
import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

/** `ssrFontSync` of contracts/build-tooling.md, section Fonts. */
class FontSyncTest {
    @TempDir
    lateinit var root: File

    private fun lockOf(fixture: ServerFontFixture): List<LockEntry> =
        LockFile(section = "fonts").read(file = fixture.lockFile)

    private fun failureOf(action: () -> Unit) = runCatching { action() }.exceptionOrNull()?.let { it::class }

    private fun <T> whileUnwritable(vararg files: File, action: () -> T): T {
        files.forEach { file -> file.setWritable(false) }
        try {
            assumeFalse(files.any { file -> file.canWrite() })
            return action()
        } finally {
            files.forEach { file -> file.setWritable(true) }
        }
    }

    @Test
    fun `given a first sync, when it runs, then it saves the whole bundle and pins fonts json in the lock`() {
        // Arrange
        val fixture = ServerFontFixture(root = root)
        val published = fixture.publish(version = VERSION, REGULAR, BOLD)

        // Act
        fixture.sync(version = VERSION)

        // Assert
        assertEquals(expected = filesOf(folder = published), actual = fixture.savedFiles(version = VERSION))
        val fingerprint = Fingerprint.of(bytes = published.resolve(FONTS_JSON).readBytes())
        val location = fixture.locationOf(version = VERSION)
        val pin = LockEntry(version = VERSION, location = location, fingerprint = fingerprint)
        assertEquals(expected = listOf(pin), actual = lockOf(fixture = fixture))
    }

    @Test
    fun `given a bundle moved to another location, when synced again, then the lock records the new location`() {
        // Arrange
        val fixture = ServerFontFixture(root = root)
        val published = fixture.publish(version = VERSION, REGULAR, BOLD)
        fixture.sync(version = VERSION)
        val movedLocation = "../app/mirror/$VERSION/fonts.json"
        published.copyRecursively(target = root.resolve("app/mirror/$VERSION"))
        val before = lockOf(fixture = fixture).single()

        // Act
        fixture.sync(version = VERSION, location = movedLocation)

        // Assert
        val expected = LockEntry(version = VERSION, location = movedLocation, fingerprint = before.fingerprint)
        assertEquals(expected = listOf(expected), actual = lockOf(fixture = fixture))
    }

    @Test
    fun `given a font file that does not match its fingerprint, when synced, then nothing is saved`() {
        // Arrange
        val fixture = ServerFontFixture(root = root)
        fixture.publish(version = VERSION, REGULAR, BOLD).resolve(BOLD_FILE).writeText("tampered")

        // Act
        val failure = failureOf { fixture.sync(version = VERSION) }

        // Assert
        assertEquals(expected = StepException::class, actual = failure)
        assertEquals(expected = emptyList(), actual = fixture.savedRoot.list().orEmpty().toList())
        assertEquals(expected = false, actual = fixture.lockFile.exists())
    }

    @Test
    fun `given a fonts json whose version differs from the declared one, when synced, then nothing is saved`() {
        // Arrange
        val fixture = ServerFontFixture(root = root)
        fixture.publish(version = VERSION, REGULAR)

        // Act
        val location = fixture.locationOf(version = VERSION)
        val failure = failureOf { fixture.sync(version = OTHER_VERSION, location = location) }

        // Assert
        assertEquals(expected = StepException::class, actual = failure)
        assertEquals(expected = false, actual = fixture.savedRoot.resolve(OTHER_VERSION).exists())
        assertEquals(expected = false, actual = fixture.lockFile.exists())
    }

    @Test
    fun `given a fonts json that is not a valid font bundle, when synced, then nothing is saved`() {
        // Arrange
        val fixture = ServerFontFixture(root = root)
        fixture.publish(version = VERSION, REGULAR).resolve(FONTS_JSON).writeText("{}")

        // Act
        val failure = failureOf { fixture.sync(version = VERSION) }

        // Assert
        assertEquals(expected = StepException::class, actual = failure)
        assertEquals(expected = false, actual = fixture.savedRoot.resolve(VERSION).exists())
    }

    @Test
    fun `given a saved copy and a changed fonts json, when synced, then it says so and fetches and changes nothing`() {
        // Arrange
        val fixture = ServerFontFixture(root = root)
        fixture.publish(version = VERSION, REGULAR)
        fixture.sync(version = VERSION)
        val savedBefore = fixture.savedFiles(version = VERSION)
        val lockBefore = fixture.lockFile.readText()
        fixture.publish(version = VERSION, REGULAR, text = "changed").resolve("brand").deleteRecursively()

        // Act
        fixture.sync(version = VERSION)

        // Assert
        assertEquals(expected = savedBefore, actual = fixture.savedFiles(version = VERSION))
        assertEquals(expected = lockBefore, actual = fixture.lockFile.readText())
        assertContains(charSequence = fixture.reports.joinToString(), other = UPDATE_OPTION)
    }

    @Test
    fun `given a saved copy and a changed fonts json, when synced with update, then both are replaced`() {
        // Arrange
        val fixture = ServerFontFixture(root = root)
        fixture.publish(version = VERSION, REGULAR)
        fixture.sync(version = VERSION)
        val republished = fixture.publish(version = VERSION, REGULAR, text = "changed")

        // Act
        fixture.sync(version = VERSION, update = true)

        // Assert
        assertEquals(expected = filesOf(folder = republished), actual = fixture.savedFiles(version = VERSION))
        val fingerprint = Fingerprint.of(bytes = republished.resolve(FONTS_JSON).readBytes())
        assertEquals(expected = listOf(fingerprint), actual = lockOf(fixture = fixture).map { it.fingerprint })
    }

    @Test
    fun `given a saved copy and an unreachable location, when synced, then the copy is kept and it says so`() {
        // Arrange
        val fixture = ServerFontFixture(root = root)
        fixture.publish(version = VERSION, REGULAR)
        fixture.sync(version = VERSION)
        val savedBefore = fixture.savedFiles(version = VERSION)
        fixture.app.root.deleteRecursively()

        // Act
        fixture.sync(version = VERSION)

        // Assert
        assertEquals(expected = savedBefore, actual = fixture.savedFiles(version = VERSION))
        assertContains(charSequence = fixture.reports.joinToString(), other = fixture.locationOf(version = VERSION))
    }

    @Test
    fun `given no saved copy and a location that cannot be reached, when synced, then the step fails`() {
        // Arrange
        val fixture = ServerFontFixture(root = root)

        // Act
        val failure = failureOf { fixture.sync(version = VERSION) }

        // Assert
        assertEquals(expected = StepException::class, actual = failure)
    }

    @Test
    fun `given an update whose font file cannot be fetched, when synced, then the saved copy and lock are kept`() {
        // Arrange
        val fixture = ServerFontFixture(root = root)
        fixture.publish(version = VERSION, REGULAR)
        fixture.sync(version = VERSION)
        val savedBefore = fixture.savedFiles(version = VERSION)
        val lockBefore = fixture.lockFile.readText()
        fixture.publish(version = VERSION, REGULAR, BOLD, text = "changed").resolve(BOLD_FILE).delete()

        // Act
        val failure = failureOf { fixture.sync(version = VERSION, update = true) }

        // Assert
        assertEquals(expected = StepException::class, actual = failure)
        assertEquals(expected = savedBefore, actual = fixture.savedFiles(version = VERSION))
        assertEquals(expected = lockBefore, actual = fixture.lockFile.readText())
    }

    @Test
    fun `given an update whose lock cannot be written, when synced, then the saved copy and the lock are kept`() {
        // Arrange
        val fixture = ServerFontFixture(root = root)
        fixture.publish(version = VERSION, REGULAR)
        fixture.sync(version = VERSION)
        val savedBefore = fixture.savedFiles(version = VERSION)
        val lockBefore = fixture.lockFile.readText()
        fixture.publish(version = VERSION, REGULAR, text = "changed")

        // Act
        val failure = whileUnwritable(fixture.lockFile, fixture.lockFile.parentFile) {
            runCatching { fixture.sync(version = VERSION, update = true) }.exceptionOrNull()
        }

        // Assert
        assertEquals(expected = true, actual = failure != null)
        assertEquals(expected = savedBefore, actual = fixture.savedFiles(version = VERSION))
        assertEquals(expected = lockBefore, actual = fixture.lockFile.readText())
        assertEquals(expected = listOf(VERSION), actual = fixture.savedRoot.list().orEmpty().toList())
    }

    @Test
    fun `given a pin with no saved folder and a matching fonts json, when synced, then the bundle is saved again`() {
        // Arrange
        val fixture = ServerFontFixture(root = root)
        val published = fixture.publish(version = VERSION, REGULAR)
        fixture.sync(version = VERSION)
        val lockBefore = fixture.lockFile.readText()
        fixture.savedRoot.resolve(VERSION).deleteRecursively()

        // Act
        fixture.sync(version = VERSION)

        // Assert
        assertEquals(expected = filesOf(folder = published), actual = fixture.savedFiles(version = VERSION))
        assertEquals(expected = lockBefore, actual = fixture.lockFile.readText())
    }

    @Test
    fun `given a pin with no saved folder and a changed fonts json, when synced, then it fails and keeps the pin`() {
        // Arrange
        val fixture = ServerFontFixture(root = root)
        fixture.publish(version = VERSION, REGULAR)
        fixture.sync(version = VERSION)
        val lockBefore = fixture.lockFile.readText()
        fixture.savedRoot.resolve(VERSION).deleteRecursively()
        fixture.publish(version = VERSION, REGULAR, text = "changed")

        // Act
        val failure = runCatching { fixture.sync(version = VERSION) }.exceptionOrNull()

        // Assert
        assertEquals(expected = StepException::class, actual = failure?.let { it::class })
        assertContains(charSequence = failure?.message.orEmpty(), other = UPDATE_OPTION)
        assertEquals(expected = false, actual = fixture.savedRoot.resolve(VERSION).exists())
        assertEquals(expected = lockBefore, actual = fixture.lockFile.readText())
    }

    private companion object {
        const val VERSION = "1"
        const val OTHER_VERSION = "2"
        const val FONTS_JSON = "fonts.json"
        const val BOLD_FILE = "brand/700-normal.ttf"
        const val UPDATE_OPTION = "--update"
    }
}
