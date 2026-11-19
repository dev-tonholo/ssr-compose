package dev.tonholo.ssrcompose.compiler.producer.scan

import java.io.File
import java.io.IOException
import java.io.Reader
import java.nio.file.FileVisitOption
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.EnumSet

/**
 * Finds component names in every readable Kotlin file under [roots], except the [excluded] files. It keeps nothing
 * between compiles.
 */
internal class SourceNameScan(
    private val roots: List<File>,
    private val excluded: Set<Path> = emptySet(),
) : ComponentNameSource {
    override fun namedAmong(names: Set<String>): Set<String> {
        val found = HashSet<String>()
        if (names.isEmpty()) return found
        val visitor = KotlinFileVisitor { file ->
            if (file.toAbsolutePath().normalize() !in excluded) found += namesIn(file = file, names = names)
            found.size < names.size
        }
        roots.forEach { root ->
            Files.walkFileTree(root.toPath(), EnumSet.of(FileVisitOption.FOLLOW_LINKS), Int.MAX_VALUE, visitor)
        }
        return found
    }

    private fun namesIn(file: Path, names: Set<String>): Set<String> =
        try {
            Files.newInputStream(file).bufferedReader().use { reader -> namesIn(reader = reader, names = names) }
        } catch (_: IOException) {
            emptySet()
        }

    private fun namesIn(reader: Reader, names: Set<String>): Set<String> {
        val found = HashSet<String>()
        val token = StringBuilder()
        val buffer = CharArray(size = BUFFER_SIZE)
        var count = reader.read(buffer)
        while (count != END_OF_INPUT) {
            repeat(times = count) { index ->
                val character = buffer[index]
                if (character.continuesName()) token.append(character) else token.moveInto(found = found, names = names)
            }
            count = reader.read(buffer)
        }
        token.moveInto(found = found, names = names)
        return found
    }

    private fun StringBuilder.moveInto(found: MutableSet<String>, names: Set<String>) {
        if (isEmpty()) return
        val name = toString()
        if (name in names) found += name
        setLength(0)
    }

    private fun Char.continuesName(): Boolean = isLetterOrDigit() || this == UNDERSCORE

    private class KotlinFileVisitor(private val onKotlinFile: (Path) -> Boolean) : SimpleFileVisitor<Path>() {
        override fun visitFile(file: Path, attributes: BasicFileAttributes): FileVisitResult {
            val isKotlin = attributes.isRegularFile &&
                file.fileName.toString().substringAfterLast(EXTENSION_SEPARATOR) in KOTLIN_EXTENSIONS
            val goesOn = !isKotlin || onKotlinFile(file)
            return if (goesOn) FileVisitResult.CONTINUE else FileVisitResult.TERMINATE
        }

        override fun visitFileFailed(file: Path, exception: IOException): FileVisitResult = FileVisitResult.CONTINUE
    }

    private companion object {
        const val UNDERSCORE = '_'
        const val EXTENSION_SEPARATOR = '.'
        const val BUFFER_SIZE = 8_192
        const val END_OF_INPUT = -1
        val KOTLIN_EXTENSIONS = setOf("kt", "kts")
    }
}
