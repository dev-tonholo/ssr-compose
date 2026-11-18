package dev.tonholo.ssrcompose.catalog.index

import dev.tonholo.ssrcompose.catalog.bundle.CatalogFileException
import dev.tonholo.ssrcompose.catalog.bundle.SizeCap
import dev.tonholo.ssrcompose.catalog.bundle.serialization.describe
import dev.tonholo.ssrcompose.catalog.index.binary.BINARY_FORMAT_VERSION
import dev.tonholo.ssrcompose.catalog.index.binary.BINARY_MAGIC
import dev.tonholo.ssrcompose.catalog.index.binary.BinaryCursor
import dev.tonholo.ssrcompose.catalog.index.binary.BodySpan
import dev.tonholo.ssrcompose.catalog.index.binary.ComponentBodyReader
import dev.tonholo.ssrcompose.catalog.index.binary.DAMAGED_INDEX
import dev.tonholo.ssrcompose.catalog.index.binary.StringTable
import dev.tonholo.ssrcompose.catalog.model.ChoiceList
import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
import java.io.InputStream

internal const val INDEX_SUBJECT = "catalog index"
private const val BYTES_PER_MEBIBYTE = 1024 * 1024
private const val DEFAULT_CAP_MEBIBYTES = 256
private const val MINIMUM_CHOICE_BYTES = 2 * Int.SIZE_BYTES
private const val MINIMUM_COMPONENT_ENTRY_BYTES = 2 * Int.SIZE_BYTES

/** Opens the index that [CatalogIndexWriter] wrote. */
public object CatalogIndexReader {
    /** The most bytes an index file may have by default, about 200 times the index of 20,000 components. */
    public val DEFAULT_SIZE_CAP: SizeCap = SizeCap(bytes = DEFAULT_CAP_MEBIBYTES * BYTES_PER_MEBIBYTE)

    /**
     * Reads an index from [stream], and refuses a stream over [sizeCap] before it is read in full. The stream is not
     * closed.
     *
     * @throws CatalogFileException when the stream is over the cap, or the index is damaged or cut short.
     */
    public fun read(stream: InputStream, sizeCap: SizeCap = DEFAULT_SIZE_CAP): CatalogIndex =
        open(bytes = sizeCap.read(stream = stream, subject = INDEX_SUBJECT))

    /**
     * Opens a copy of the index held in [bytes], so later changes to [bytes] do not reach the opened index.
     *
     * @throws CatalogFileException when the index is damaged or cut short.
     */
    public fun read(bytes: ByteArray): CatalogIndex = open(bytes = bytes.copyOf())

    private fun open(bytes: ByteArray): CatalogIndex = refusingDamage {
        val cursor = BinaryCursor(bytes = bytes)
        require(cursor.int() == BINARY_MAGIC && cursor.byte() == BINARY_FORMAT_VERSION) { DAMAGED_INDEX }
        val versionId = cursor.int()
        val strings = StringTable.read(cursor = cursor, bytes = bytes)
        val choices = readChoices(cursor = cursor, strings = strings)
        val offsets = readBodyOffsets(cursor = cursor, strings = strings)
        val bodiesLength = cursor.int()
        require(bodiesLength == cursor.remaining) { DAMAGED_INDEX }
        CatalogIndex(
            version = CatalogVersion(value = strings[versionId]),
            choices = choices,
            bodySpans = bodySpansOf(offsets = offsets, bodiesLength = bodiesLength),
            bodies = ComponentBodyReader(
                bytes = bytes,
                strings = strings,
                bodiesStart = cursor.position,
                choiceListNames = choices.mapTo(destination = HashSet()) { it.name },
            ),
        )
    }

    private fun bodySpansOf(offsets: Map<String, Int>, bodiesLength: Int): Map<String, BodySpan> {
        val boundaries = offsets.values.toList() + bodiesLength
        require(boundaries.first() == 0) { DAMAGED_INDEX }
        require(boundaries.zipWithNext().all { (start, end) -> start < end }) { DAMAGED_INDEX }
        return offsets.keys.withIndex().associate { (position, name) ->
            name to BodySpan(start = boundaries[position], end = boundaries[position + 1])
        }
    }

    private fun readChoices(cursor: BinaryCursor, strings: StringTable): List<ChoiceList> {
        val choices = List(size = cursor.count(minimumElementBytes = MINIMUM_CHOICE_BYTES)) {
            ChoiceList(
                name = strings[cursor.int()],
                entries = List(size = cursor.count(minimumElementBytes = Int.SIZE_BYTES)) { strings[cursor.int()] },
            )
        }
        require(choices.distinctBy { it.name }.size == choices.size) { DAMAGED_INDEX }
        return choices
    }

    private fun readBodyOffsets(cursor: BinaryCursor, strings: StringTable): Map<String, Int> {
        val offsets = LinkedHashMap<String, Int>()
        var previousName = ""
        repeat(times = cursor.count(minimumElementBytes = MINIMUM_COMPONENT_ENTRY_BYTES)) {
            val name = strings[cursor.int()]
            require(previousName < name) { DAMAGED_INDEX }
            offsets[name] = cursor.int()
            previousName = name
        }
        return offsets
    }
}

internal inline fun <T> refusingDamage(read: () -> T): T = try {
    read()
} catch (exception: IllegalArgumentException) {
    throw CatalogFileException(message = "The $INDEX_SUBJECT is not valid: ${describe(exception = exception)}")
}
