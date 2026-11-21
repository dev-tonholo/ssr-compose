package dev.tonholo.ssrcompose.tooling.lock

import dev.tonholo.ssrcompose.tooling.StepException
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

/** The lock file shape of contracts/build-tooling.md, shared by `ssr/catalogs.lock` and `ssr/fonts.lock`. */
class LockFileTest {
    @TempDir
    lateinit var folder: File

    private val lockFile: File
        get() = folder.resolve("ssr/fonts.lock")

    private val entries = listOf(
        LockEntry(version = "2", location = SECOND_LOCATION, fingerprint = FINGERPRINT_B),
        LockEntry(version = "1", location = FIRST_LOCATION, fingerprint = FINGERPRINT_A),
    )

    private fun failureOf(text: String) = runCatching {
        lockFile.apply { parentFile.mkdirs() }.writeText(text = text)
        LockFile(section = SECTION).read(file = lockFile)
    }.exceptionOrNull()?.let { failure -> failure::class }

    @Test
    fun `given entries in any order, when written, then the bytes are the sorted golden form`() {
        // Arrange
        val lock = LockFile(section = SECTION)

        // Act
        lock.write(file = lockFile, entries = entries)

        // Assert
        assertEquals(expected = GOLDEN, actual = lockFile.readText())
    }

    @Test
    fun `given a reader of the old lock, when a new lock is written, then the reader still sees the old lock whole`() {
        // Arrange
        val lock = LockFile(section = SECTION)
        lock.write(file = lockFile, entries = entries.take(n = 1))
        val oldText = lockFile.readText()

        // Act
        val seenByReader = lockFile.inputStream().use { reader ->
            lock.write(file = lockFile, entries = entries)
            reader.readAllBytes().decodeToString()
        }

        // Assert
        assertEquals(expected = oldText, actual = seenByReader)
        assertEquals(expected = entries.sortedBy { it.version }, actual = lock.read(file = lockFile))
    }

    @Test
    fun `given a staging file of another writer, when a lock is written, then that file is left alone`() {
        // Arrange
        val otherWriters = lockFile.parentFile.apply { mkdirs() }.resolve(".fonts.lock.staging")
            .apply { writeText(text = "another writer") }

        // Act
        LockFile(section = SECTION).write(file = lockFile, entries = entries)

        // Assert
        assertEquals(expected = "another writer", actual = otherWriters.readText())
    }

    @Test
    fun `given a write that fails, when it is done, then no other file is left next to the lock`() {
        // Arrange
        val kept = lockFile.apply { mkdirs() }.resolve("keep.txt").apply { writeText(text = "keep") }
        val before = lockFile.parentFile.list().orEmpty().sorted()

        // Act
        val failure = runCatching { LockFile(section = SECTION).write(file = lockFile, entries = entries) }

        // Assert
        assertEquals(expected = true, actual = failure.isFailure)
        assertEquals(expected = before, actual = lockFile.parentFile.list().orEmpty().sorted())
        assertEquals(expected = "keep", actual = kept.readText())
    }

    @Test
    fun `given a written lock, when read, then it gives the entries sorted by version`() {
        // Arrange
        val lock = LockFile(section = SECTION)
        lock.write(file = lockFile, entries = entries)

        // Act
        val actual = lock.read(file = lockFile)

        // Assert
        assertEquals(expected = entries.sortedBy { it.version }, actual = actual)
    }

    @Test
    fun `given no lock file, when read, then it has no entries`() {
        // Arrange
        val lock = LockFile(section = SECTION)

        // Act
        val actual = lock.read(file = lockFile)

        // Assert
        assertEquals(expected = emptyList(), actual = actual)
    }

    @Test
    fun `given a lock of another section, a repeated version or a bad shape, when read, then it is refused`() {
        // Arrange
        val texts = listOf(
            /* language=json */ """{"catalogs":[]}""",
            /* language=json */ """{"fonts":[],"catalogs":[]}""",
            // language=json
            """
            {"fonts":[{"version":"1","location":"a","fingerprint":"b"},
            {"version":"1","location":"c","fingerprint":"d"}]}
            """,
            /* language=json */ """{"fonts":[{"version":"1","location":"a"}]}""",
            "not json",
        )

        // Act
        val actual = texts.map { text -> failureOf(text = text) }

        // Assert
        assertEquals(expected = texts.map { StepException::class }, actual = actual)
    }

    @Test
    fun `given a lock file of exactly the size cap, when read, then it is read`() {
        // Arrange
        lockFile.apply { parentFile.mkdirs() }.writeText(text = lockPaddedTo(size = LOCK_SIZE_CAP))

        // Act
        val actual = LockFile(section = SECTION).read(file = lockFile)

        // Assert
        assertEquals(expected = emptyList(), actual = actual)
    }

    @Test
    fun `given a lock file one byte over the size cap, when read, then it is refused as over its size cap`() {
        // Arrange
        lockFile.apply { parentFile.mkdirs() }.writeText(text = lockPaddedTo(size = LOCK_SIZE_CAP + 1))

        // Act
        val failure = runCatching { LockFile(section = SECTION).read(file = lockFile) }.exceptionOrNull()

        // Assert
        assertEquals(expected = StepException::class, actual = failure?.let { it::class })
        assertContains(charSequence = failure?.message.orEmpty(), other = "over its size cap")
    }

    private fun lockPaddedTo(size: Int): String = EMPTY_LOCK.padEnd(length = size, padChar = ' ')

    private companion object {
        const val LOCK_SIZE_CAP = 1_048_576

        // language=json
        const val EMPTY_LOCK = """{"fonts":[]}"""
        const val SECTION = "fonts"
        const val FIRST_LOCATION = "https://example.com/ssr/fonts/1/fonts.json"
        const val SECOND_LOCATION = "../app/ssr/fonts/2/fonts.json"
        const val FINGERPRINT_A = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
        const val FINGERPRINT_B = "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210"

        // language=json
        const val GOLDEN = """{
  "fonts": [
    {
      "version": "1",
      "location": "$FIRST_LOCATION",
      "fingerprint": "$FINGERPRINT_A"
    },
    {
      "version": "2",
      "location": "$SECOND_LOCATION",
      "fingerprint": "$FINGERPRINT_B"
    }
  ]
}
"""
    }
}
