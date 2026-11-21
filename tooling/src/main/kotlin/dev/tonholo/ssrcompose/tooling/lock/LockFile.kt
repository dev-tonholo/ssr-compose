package dev.tonholo.ssrcompose.tooling.lock

import dev.tonholo.ssrcompose.tooling.StepException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.UUID

private const val INDENT = "  "
private const val TRAILING_NEWLINE = "\n"
private const val SIZE_CAP_BYTES = 1_048_576L
private const val HIDDEN_PREFIX = "."
private const val STAGING_SUFFIX = ".staging"

@OptIn(ExperimentalSerializationApi::class)
private val lockJson = Json {
    prettyPrint = true
    prettyPrintIndent = INDENT
}

private val lockSerializer = MapSerializer(
    keySerializer = String.serializer(),
    valueSerializer = ListSerializer(elementSerializer = LockEntryFile.serializer()),
)

/**
 * A lock file of contracts/build-tooling.md: one [section], such as `catalogs` or `fonts`, that lists one
 * [LockEntry] per held version, sorted by version.
 */
public class LockFile(private val section: String) {
    /**
     * @return the entries of [file], sorted by version, or none when the file does not exist.
     * @throws StepException when the file is over 1 MiB, is not a lock file of this section or repeats a version.
     */
    public fun read(file: File): List<LockEntry> {
        if (!file.exists()) return emptyList()
        requireWithinCap(file = file)
        val sections = try {
            lockJson.decodeFromString(deserializer = lockSerializer, string = file.readText())
        } catch (cause: SerializationException) {
            throw StepException(message = "The lock file $file is not valid", cause = cause)
        }
        val entries = sections[section]
        if (sections.keys != setOf(section) || entries == null) {
            throw StepException(message = "The lock file $file must list \"$section\" and nothing else")
        }
        return entries.map { it.toEntry() }.sortedBy { it.version }.also { requireDistinct(entries = it, file = file) }
    }

    /**
     * Writes [entries] to [file] whole or not at all: the bytes go to a file of its own next to it, which then takes
     * the lock's place in one step. When that fails, [file] is as it was and that file is removed. Two runs that
     * read the lock and then write it can still lose one update; that needs a file lock.
     */
    public fun write(file: File, entries: List<LockEntry>) {
        val sections = mapOf(section to entries.sortedBy { it.version }.map { LockEntryFile.of(entry = it) })
        val text = lockJson.encodeToString(serializer = lockSerializer, value = sections) + TRAILING_NEWLINE
        val folder = file.absoluteFile.parentFile.apply { mkdirs() }.toPath()
        val staged = folder.resolve("$HIDDEN_PREFIX${file.name}.${UUID.randomUUID()}$STAGING_SUFFIX")
        try {
            Files.write(staged, text.encodeToByteArray(), StandardOpenOption.CREATE_NEW)
            Files.move(staged, file.toPath(), StandardCopyOption.ATOMIC_MOVE)
        } finally {
            Files.deleteIfExists(staged)
        }
    }

    private fun requireWithinCap(file: File) {
        if (file.length() > SIZE_CAP_BYTES) throw StepException(message = "The lock file $file is over its size cap")
    }

    private fun requireDistinct(entries: List<LockEntry>, file: File) {
        val repeated = entries.groupBy { it.version }.filterValues { it.size > 1 }.keys
        if (repeated.isNotEmpty()) {
            throw StepException(message = "The lock file $file lists the versions $repeated more than once")
        }
    }
}
