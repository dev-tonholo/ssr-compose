package dev.tonholo.ssrcompose.consumer.internal.read

import dev.tonholo.ssrcompose.consumer.PlacedComponentScope
import dev.tonholo.ssrcompose.consumer.PlacedValueException
import dev.tonholo.ssrcompose.core.data.DataValue

internal inline fun <reified V : DataValue> PlacedComponentScope.valueOrNull(name: String, expected: String): V? =
    when (val value = placed.values[name]) {
        null, DataValue.None -> null
        is V -> value
        else -> failedRead(
            problem = wrongKind(component = placed.name, parameter = name, expected = expected, found = value),
            standIn = null,
        )
    }

internal fun <T> PlacedComponentScope.absent(name: String, expected: String, standIn: T): T = failedRead(
    problem = absentValue(component = placed.name, parameter = name, expected = expected),
    standIn = standIn,
)

@PublishedApi
internal fun PlacedComponentScope.choiceEntryOrNull(name: String): String? =
    valueOrNull<DataValue.Choice>(name = name, expected = KindName.CHOICE)?.entry

/**
 * The stand-in of a choice is the first entry of [entries]. An enum with no entries has no value to stand in,
 * so the read throws even inside a session.
 */
@PublishedApi
internal fun <E : Enum<E>> PlacedComponentScope.absentChoice(name: String, entries: Array<E>): E {
    val problem = absentValue(component = placed.name, parameter = name, expected = KindName.CHOICE)
    val standIn = entries.firstOrNull()
    if (standIn == null) {
        problems?.report(placed = placed)
        throw problem
    }
    return failedRead(problem = problem, standIn = standIn)
}

@PublishedApi
internal fun <E : Enum<E>> PlacedComponentScope.entryOf(
    name: String,
    entry: String,
    entries: Array<E>,
    enumName: String,
): E? = entries.firstOrNull { it.name == entry } ?: failedRead(
    problem = unknownEntry(component = placed.name, parameter = name, entry = entry, enumName = enumName),
    standIn = null,
)

/** Throws [problem] outside a session. Inside one, it reports the component and gives [standIn] to the binding. */
internal fun <T> PlacedComponentScope.failedRead(problem: PlacedValueException, standIn: T): T {
    val reporter = problems ?: throw problem
    reporter.report(placed = placed)
    return standIn
}
