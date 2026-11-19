package dev.tonholo.ssrcompose.compiler.producer.scan

import org.junit.jupiter.api.Assumptions.assumeFalse
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class SourceNameScanTest {
    @TempDir
    lateinit var root: File

    @Test
    fun `given a call in a nested folder, when scanned, then its name is found`() {
        // Arrange
        // language=kotlin
        val source = "fun screen() { Greeting(title = 1) }"
        write(path = "app/screens/deep/Screen.kt", text = source)

        // Act
        val named = scan(names = setOf(GREETING, CARD))

        // Assert
        assertEquals(expected = setOf(GREETING), actual = named)
    }

    @Test
    fun `given a longer identifier that holds a name, when scanned, then the name is not found`() {
        // Arrange
        // language=kotlin
        val source = "val first = MyGreeting; val second = GreetingCard; val third = Greeting2"
        write(path = "Screen.kt", text = source)

        // Act
        val named = scan(names = setOf(GREETING))

        // Assert
        assertEquals(expected = emptySet(), actual = named)
    }

    @Test
    fun `given a name in backticks, when scanned, then it is found`() {
        // Arrange
        // language=kotlin
        val source = "fun screen() { `Greeting`() }"
        write(path = "Screen.kt", text = source)

        // Act
        val named = scan(names = setOf(GREETING))

        // Assert
        assertEquals(expected = setOf(GREETING), actual = named)
    }

    @Test
    fun `given a name right after a callable reference, when scanned, then it is found`() {
        // Arrange
        // language=kotlin
        val source = "val reference = ::Greeting"
        write(path = "Screen.kt", text = source)

        // Act
        val named = scan(names = setOf(GREETING))

        // Assert
        assertEquals(expected = setOf(GREETING), actual = named)
    }

    @Test
    fun `given a name at the very end of a file, when scanned, then it is found`() {
        // Arrange
        // language=kotlin
        val source = "import app.bridge.Greeting"
        write(path = "Screen.kt", text = source)

        // Act
        val named = scan(names = setOf(GREETING))

        // Assert
        assertEquals(expected = setOf(GREETING), actual = named)
    }

    @Test
    fun `given a name that is not ASCII, when scanned, then it is found`() {
        // Arrange
        // language=kotlin
        val source = "fun screen() { Título() }"
        write(path = "Screen.kt", text = source)

        // Act
        val named = scan(names = setOf(NOT_ASCII))

        // Assert
        assertEquals(expected = setOf(NOT_ASCII), actual = named)
    }

    @Test
    fun `given a name only in a file that is not Kotlin, when scanned, then it is not found`() {
        // Arrange
        write(path = "notes.txt", text = "Greeting")

        // Act
        val named = scan(names = setOf(GREETING))

        // Assert
        assertEquals(expected = emptySet(), actual = named)
    }

    @Test
    fun `given names in two roots, when scanned, then both are found`() {
        // Arrange
        // language=kotlin
        val first = "val a = Greeting"
        // language=kotlin
        val second = "val b = Card"
        write(path = "first/Screen.kt", text = first)
        write(path = "second/Screen.kt", text = second)
        val roots = listOf(root.resolve("first"), root.resolve("second"))

        // Act
        val named = SourceNameScan(roots = roots).namedAmong(names = setOf(GREETING, CARD))

        // Assert
        assertEquals(expected = setOf(GREETING, CARD), actual = named)
    }

    @Test
    fun `given a root that is one file, when scanned, then its names are found`() {
        // Arrange
        val file = write(path = "Screen.kt", text = MENTION)

        // Act
        val named = SourceNameScan(roots = listOf(file)).namedAmong(names = setOf(GREETING))

        // Assert
        assertEquals(expected = setOf(GREETING), actual = named)
    }

    @Test
    fun `given a root that does not exist, when scanned, then nothing is found`() {
        // Arrange
        val missing = root.resolve("missing")

        // Act
        val named = SourceNameScan(roots = listOf(missing)).namedAmong(names = setOf(GREETING))

        // Assert
        assertEquals(expected = emptySet(), actual = named)
    }

    @Test
    fun `given a linked source folder, when scanned, then the names inside it are found`() {
        // Arrange
        write(path = "outside/Screen.kt", text = MENTION)
        val sources = root.resolve("sources").apply { mkdirs() }
        Files.createSymbolicLink(sources.resolve("linked").toPath(), root.resolve("outside").toPath())

        // Act
        val named = SourceNameScan(roots = listOf(sources)).namedAmong(names = setOf(GREETING))

        // Assert
        assertEquals(expected = setOf(GREETING), actual = named)
    }

    @Test
    fun `given a folder link that loops, when scanned, then the scan ends and finds the names`() {
        // Arrange
        write(path = "sources/Screen.kt", text = MENTION)
        val sources = root.resolve("sources")
        Files.createSymbolicLink(sources.resolve("loop").toPath(), sources.toPath())

        // Act
        val named = SourceNameScan(roots = listOf(sources)).namedAmong(names = setOf(GREETING, CARD))

        // Assert
        assertEquals(expected = setOf(GREETING), actual = named)
    }

    @Test
    fun `given a name only in an excluded file, when scanned, then it is not found`() {
        // Arrange
        val excluded = write(path = "Excluded.kt", text = MENTION)
        val excludedFiles = setOf(excluded.toPath().toAbsolutePath().normalize())

        // Act
        val named = SourceNameScan(roots = listOf(root), excluded = excludedFiles).namedAmong(names = setOf(GREETING))

        // Assert
        assertEquals(expected = emptySet(), actual = named)
    }

    @Test
    fun `given a name in a Kotlin script, when scanned, then it is found`() {
        // Arrange
        write(path = "build.kts", text = MENTION)

        // Act
        val named = scan(names = setOf(GREETING))

        // Assert
        assertEquals(expected = setOf(GREETING), actual = named)
    }

    @Test
    fun `given a dangling link named like a Kotlin file, when scanned, then the other files are still read`() {
        // Arrange
        write(path = "Screen.kt", text = MENTION)
        Files.createSymbolicLink(root.resolve(".#A.kt").toPath(), root.resolve("missing-target").toPath())

        // Act
        val named = scan(names = setOf(GREETING, CARD))

        // Assert
        assertEquals(expected = setOf(GREETING), actual = named)
    }

    @Test
    fun `given a named pipe named like a Kotlin file, when scanned, then it is not read`() {
        // Arrange
        assumeTrue(canStart(command = MAKE_PIPE), "This host has no $MAKE_PIPE")
        assumeTrue(canStart(command = SHELL), "This host has no $SHELL")
        val pipe = root.resolve("Pipe.kt")
        check(ProcessBuilder(MAKE_PIPE, pipe.path).start().waitFor() == 0) { "The test cannot create a named pipe" }
        val writer = ProcessBuilder(SHELL, "-c", WRITE_TO_PIPE, SHELL, MENTION, pipe.path).start()
        try {
            // Act
            val named = scan(names = setOf(GREETING))

            // Assert
            assertEquals(expected = emptySet(), actual = named)
        } finally {
            writer.destroyForcibly().waitFor()
        }
    }

    @Test
    fun `given a Kotlin file that cannot be read, when scanned, then nothing is found in it`() {
        // Arrange
        write(path = "Screen.kt", text = MENTION)
        // language=kotlin
        val lockedSource = "val b = Card"
        val locked = write(path = "Locked.kt", text = lockedSource)
        locked.setReadable(false)
        try {
            assumeFalse(locked.canRead(), "This process can read a file without read permission")

            // Act
            val named = scan(names = setOf(GREETING, CARD))

            // Assert
            assertEquals(expected = setOf(GREETING), actual = named)
        } finally {
            locked.setReadable(true)
        }
    }

    @Test
    fun `given a file that is not valid UTF-8, when scanned, then the names around the bad bytes are found`() {
        // Arrange
        // language=kotlin
        val before = "val a = Card "
        // language=kotlin
        val after = " val b = Greeting"
        val bytes = before.encodeToByteArray() + INVALID_UTF8 + after.encodeToByteArray()
        root.resolve("Broken.kt").writeBytes(array = bytes)

        // Act
        val named = scan(names = setOf(GREETING, CARD))

        // Assert
        assertEquals(expected = setOf(GREETING, CARD), actual = named)
    }

    @Test
    fun `given a name far into a large file, when scanned, then it is found whole`() {
        // Arrange
        val padding = " ".repeat(n = LARGE_FILE_PADDING)
        write(path = "Large.kt", text = "$padding$MENTION")

        // Act
        val named = scan(names = setOf(GREETING))

        // Assert
        assertEquals(expected = setOf(GREETING), actual = named)
    }

    private fun canStart(command: String): Boolean =
        try {
            val process = ProcessBuilder(command).redirectErrorStream(true).start()
            process.outputStream.close()
            process.inputStream.readAllBytes()
            process.waitFor()
            true
        } catch (_: IOException) {
            false
        }

    private fun scan(names: Set<String>): Set<String> =
        SourceNameScan(roots = listOf(root)).namedAmong(names = names)

    private fun write(path: String, text: String): File =
        root.resolve(path).apply {
            parentFile.mkdirs()
            writeText(text = text)
        }

    private companion object {
        const val GREETING = "Greeting"
        const val CARD = "Card"
        const val NOT_ASCII = "Título"
        const val LARGE_FILE_PADDING = 8_180
        const val MAKE_PIPE = "mkfifo"
        const val SHELL = "sh"

        // language=shell
        const val WRITE_TO_PIPE = "printf '%s' \"$1\" > \"$2\""

        // language=kotlin
        const val MENTION = "val a = Greeting"
        val INVALID_UTF8 = byteArrayOf(0xC3.toByte(), 0x28)
    }
}
