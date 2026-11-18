package dev.tonholo.ssrcompose.catalog.testing.indexformat

import dev.tonholo.ssrcompose.catalog.model.ChoiceList
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.catalog.model.SlotAccepts
import dev.tonholo.ssrcompose.core.data.ValueKind

private const val MINIMUM_PARAMETER_BYTES = 7
private const val MINIMUM_CHOICE_BYTES = 2 * Int.SIZE_BYTES
private const val MINIMUM_COMPONENT_ENTRY_BYTES = 2 * Int.SIZE_BYTES

internal class BinaryIndex private constructor(
    private val bytes: ByteArray,
    private val strings: StringTable,
    override val version: String,
    override val choices: List<ChoiceList>,
    private val bodyOffsets: Map<String, Int>,
    private val bodiesStart: Int,
) : CandidateIndex {
    override val componentNames: List<String> = bodyOffsets.keys.toList()

    override fun component(name: String): ComponentDescriptor? {
        val offset = bodyOffsets[name] ?: return null
        val cursor = BinaryCursor(bytes = bytes, start = bodiesStart + offset)
        return ComponentDescriptor(
            name = name,
            parameters = List(size = cursor.count(minimumElementBytes = MINIMUM_PARAMETER_BYTES)) {
                readParameter(cursor = cursor)
            },
        )
    }

    private fun readParameter(cursor: BinaryCursor): Parameter {
        val tag = cursor.byte()
        val name = strings[cursor.int()]
        val flags = cursor.byte()
        val required = flags and FLAG_REQUIRED != 0
        return when (tag) {
            PARAMETER_VALUE -> Parameter.Value(
                name = name,
                kind = readKind(cursor = cursor),
                required = required,
                nullable = flags and FLAG_NULLABLE != 0,
            )

            PARAMETER_CALLBACK -> Parameter.Callback(
                name = name,
                arguments = readKinds(cursor = cursor),
                required = required,
            )

            PARAMETER_SLOT -> Parameter.Slot(
                name = name,
                accepts = requireNotNull(SlotAccepts.entries.getOrNull(cursor.byte())) { NOT_A_BINARY_INDEX },
                arguments = readKinds(cursor = cursor),
                required = required,
            )

            else -> throw IllegalArgumentException("Unknown parameter tag $tag")
        }
    }

    private fun readKinds(cursor: BinaryCursor): List<ValueKind> =
        List(size = cursor.count(minimumElementBytes = Byte.SIZE_BYTES)) { readKind(cursor = cursor) }

    private fun readKind(cursor: BinaryCursor, insideList: Boolean = false): ValueKind =
        when (val tag = cursor.byte()) {
            KIND_TEXT -> ValueKind.Text
            KIND_WHOLE -> ValueKind.Whole
            KIND_DECIMAL -> ValueKind.Decimal
            KIND_FLAG -> ValueKind.Flag
            KIND_BYTES -> ValueKind.Bytes
            KIND_CHOICE -> ValueKind.Choice(listName = strings[cursor.int()])
            KIND_LIST -> {
                require(!insideList) { NOT_A_BINARY_INDEX }
                ValueKind.ListOf(element = readKind(cursor = cursor, insideList = true))
            }

            else -> throw IllegalArgumentException("Unknown kind tag $tag")
        }

    companion object {
        fun open(bytes: ByteArray): BinaryIndex {
            val cursor = BinaryCursor(bytes = bytes)
            require(cursor.int() == BINARY_MAGIC && cursor.byte() == BINARY_FORMAT_VERSION) { NOT_A_BINARY_INDEX }
            val versionId = cursor.int()
            val strings = StringTable.read(cursor = cursor, bytes = bytes)
            val choices = readChoices(cursor = cursor, strings = strings)
            val offsets = readBodyOffsets(cursor = cursor, strings = strings)
            val bodiesLength = cursor.int()
            require(bodiesLength == cursor.remaining) { NOT_A_BINARY_INDEX }
            require(offsets.values.all { offset -> offset in 0 until bodiesLength }) { NOT_A_BINARY_INDEX }
            return BinaryIndex(
                bytes = bytes,
                strings = strings,
                version = strings[versionId],
                choices = choices,
                bodyOffsets = offsets,
                bodiesStart = cursor.position,
            )
        }

        private fun readChoices(cursor: BinaryCursor, strings: StringTable): List<ChoiceList> =
            List(size = cursor.count(minimumElementBytes = MINIMUM_CHOICE_BYTES)) {
                ChoiceList(
                    name = strings[cursor.int()],
                    entries = List(size = cursor.count(minimumElementBytes = Int.SIZE_BYTES)) {
                        strings[cursor.int()]
                    },
                )
            }

        private fun readBodyOffsets(cursor: BinaryCursor, strings: StringTable): Map<String, Int> {
            val count = cursor.count(minimumElementBytes = MINIMUM_COMPONENT_ENTRY_BYTES)
            val offsets = LinkedHashMap<String, Int>(count * 2)
            repeat(times = count) {
                val name = strings[cursor.int()]
                require(offsets.put(name, cursor.int()) == null) { NOT_A_BINARY_INDEX }
            }
            return offsets
        }
    }
}

internal class StringTable private constructor(
    private val bytes: ByteArray,
    private val offsets: IntArray,
    private val start: Int,
) {
    operator fun get(id: Int): String {
        require(id in 0 until offsets.lastIndex) { NOT_A_BINARY_INDEX }
        return String(
            bytes = bytes,
            offset = start + offsets[id],
            length = offsets[id + 1] - offsets[id],
            charset = Charsets.UTF_8,
        )
    }

    companion object {
        fun read(cursor: BinaryCursor, bytes: ByteArray): StringTable {
            val count = cursor.count(minimumElementBytes = Int.SIZE_BYTES)
            val offsets = IntArray(size = count + 1) { cursor.int() }
            val inOrder = offsets.asList().zipWithNext().all { (first, next) -> first <= next }
            require(offsets.first() == 0 && inOrder) { NOT_A_BINARY_INDEX }
            val start = cursor.position
            cursor.skip(byteCount = offsets.last())
            return StringTable(bytes = bytes, offsets = offsets, start = start)
        }
    }
}
