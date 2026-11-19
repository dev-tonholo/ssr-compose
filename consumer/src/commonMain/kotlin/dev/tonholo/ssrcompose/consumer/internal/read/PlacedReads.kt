package dev.tonholo.ssrcompose.consumer.internal.read

import dev.tonholo.ssrcompose.consumer.PlacedComponentScope
import dev.tonholo.ssrcompose.core.data.DataValue

internal inline fun <reified V : DataValue> PlacedComponentScope.valueOrNull(name: String, expected: String): V? =
    when (val value = placed.values[name]) {
        null, DataValue.None -> null
        is V -> value
        else -> throw wrongKind(component = placed.name, parameter = name, expected = expected, found = value)
    }

internal fun PlacedComponentScope.absent(name: String, expected: String): Nothing =
    throw absentValue(component = placed.name, parameter = name, expected = expected)

@PublishedApi
internal fun PlacedComponentScope.choiceEntryOrNull(name: String): String? =
    valueOrNull<DataValue.Choice>(name = name, expected = KindName.CHOICE)?.entry

@PublishedApi
internal fun PlacedComponentScope.absentChoice(name: String): Nothing = absent(name = name, expected = KindName.CHOICE)

@PublishedApi
internal fun <E : Enum<E>> PlacedComponentScope.entryOf(
    name: String,
    entry: String,
    entries: Array<E>,
    enumName: String,
): E = entries.firstOrNull { it.name == entry }
    ?: throw unknownEntry(component = placed.name, parameter = name, entry = entry, enumName = enumName)
