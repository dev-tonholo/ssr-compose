package dev.tonholo.ssrcompose.catalog.index.binary

import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.catalog.model.SlotAccepts
import dev.tonholo.ssrcompose.catalog.validation.CatalogLimits
import dev.tonholo.ssrcompose.core.data.ValueKind

private const val MINIMUM_PARAMETER_BYTES = 7

/** Decodes the parameters of one component from the bodies of an index. */
internal class ComponentBodyReader(
    private val bytes: ByteArray,
    private val strings: StringTable,
    private val bodiesStart: Int,
    private val choiceListNames: Set<String>,
) {
    /** @throws IllegalArgumentException when the parameters do not fill [span] exactly. */
    fun read(name: String, span: BodySpan): ComponentDescriptor {
        val cursor = BinaryCursor(bytes = bytes, start = bodiesStart + span.start, end = bodiesStart + span.end)
        val parameterCount = cursor.count(minimumElementBytes = MINIMUM_PARAMETER_BYTES)
        val parameters = List(size = parameterCount) { readParameter(cursor = cursor) }
        require(cursor.remaining == 0) { DAMAGED_INDEX }
        return ComponentDescriptor(name = name, parameters = parameters)
    }

    private fun readParameter(cursor: BinaryCursor): Parameter {
        val tag = cursor.byte()
        val name = strings[cursor.int()]
        val flags = cursor.byte()
        require(flags and KNOWN_FLAGS.inv() == 0) { DAMAGED_INDEX }
        require(tag == PARAMETER_VALUE || flags and FLAG_NULLABLE == 0) { DAMAGED_INDEX }
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
                arguments = readKinds(cursor = cursor, maximum = CatalogLimits.MAX_CALLBACK_ARGUMENTS),
                required = required,
            )

            PARAMETER_SLOT -> Parameter.Slot(
                name = name,
                accepts = readAccepts(cursor = cursor),
                arguments = readKinds(cursor = cursor),
                required = required,
            )

            else -> throw IllegalArgumentException(DAMAGED_INDEX)
        }
    }

    private fun readAccepts(cursor: BinaryCursor): SlotAccepts =
        when (cursor.byte()) {
            ACCEPTS_ANY -> SlotAccepts.Any
            ACCEPTS_CONSUMER -> SlotAccepts.Consumer
            ACCEPTS_PRODUCER -> SlotAccepts.Producer
            else -> throw IllegalArgumentException(DAMAGED_INDEX)
        }

    private fun readKinds(cursor: BinaryCursor, maximum: Int = Int.MAX_VALUE): List<ValueKind> {
        val count = cursor.count(minimumElementBytes = Byte.SIZE_BYTES)
        require(count <= maximum) { DAMAGED_INDEX }
        return List(size = count) { readKind(cursor = cursor) }
    }

    private fun readKnownChoiceListName(cursor: BinaryCursor): String {
        val listName = strings[cursor.int()]
        require(listName in choiceListNames) { DAMAGED_INDEX }
        return listName
    }

    private fun readKind(cursor: BinaryCursor, insideList: Boolean = false): ValueKind =
        when (cursor.byte()) {
            KIND_TEXT -> ValueKind.Text
            KIND_WHOLE -> ValueKind.Whole
            KIND_DECIMAL -> ValueKind.Decimal
            KIND_FLAG -> ValueKind.Flag
            KIND_BYTES -> ValueKind.Bytes
            KIND_CHOICE -> ValueKind.Choice(listName = readKnownChoiceListName(cursor = cursor))
            KIND_LIST -> {
                require(!insideList) { DAMAGED_INDEX }
                ValueKind.ListOf(element = readKind(cursor = cursor, insideList = true))
            }

            else -> throw IllegalArgumentException(DAMAGED_INDEX)
        }
}
