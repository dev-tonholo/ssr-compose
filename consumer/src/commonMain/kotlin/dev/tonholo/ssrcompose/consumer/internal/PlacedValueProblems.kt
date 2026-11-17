package dev.tonholo.ssrcompose.consumer.internal

import dev.tonholo.ssrcompose.consumer.PlacedValueException
import dev.tonholo.ssrcompose.core.data.DataValue

internal fun absentValue(component: String, parameter: String, expected: String): PlacedValueException =
    PlacedValueException(message = "$component.$parameter needs a $expected value but the screen gave none")

internal fun wrongKind(component: String, parameter: String, expected: String, found: DataValue): PlacedValueException =
    PlacedValueException(
        message = "$component.$parameter needs a $expected value but the screen gave a ${found.kindName()} value",
    )

internal fun unknownEntry(component: String, parameter: String, entry: String, enumName: String): PlacedValueException =
    PlacedValueException(
        message = "$component.$parameter holds the entry $entry, which the enum $enumName does not have",
    )
