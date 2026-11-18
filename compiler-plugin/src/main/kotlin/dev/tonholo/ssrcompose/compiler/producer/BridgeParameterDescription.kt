package dev.tonholo.ssrcompose.compiler.producer

import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.core.data.ValueKind

private const val MAX_KINDS_IN_MESSAGE = 4

/** Describes the shape of this parameter for an error message, in a length that does not grow with its arguments. */
internal fun Parameter.shapeDescription(): String = when (this) {
    is Parameter.Value ->
        "of kind ${kind.label()}, ${requirement()}, ${if (nullable) "nullable" else "not nullable"},"

    is Parameter.Callback -> "with ${argumentSummary(kinds = arguments)} callback arguments, ${requirement()},"
    is Parameter.Slot -> "with ${argumentSummary(kinds = arguments)} slot arguments, ${requirement()},"
}

private fun Parameter.requirement(): String = if (required) "required" else "optional"

private fun argumentSummary(kinds: List<ValueKind>): String {
    val shown = kinds.take(n = MAX_KINDS_IN_MESSAGE).joinToString { kind -> kind.label() }
    val hidden = kinds.size - MAX_KINDS_IN_MESSAGE
    val listed = if (hidden > 0) "$shown and $hidden more" else shown
    return "${kinds.size} ($listed)"
}

private fun ValueKind.label(): String = when (this) {
    ValueKind.Text -> "Text"
    ValueKind.Whole -> "Whole"
    ValueKind.Decimal -> "Decimal"
    ValueKind.Flag -> "Flag"
    ValueKind.Bytes -> "Bytes"
    is ValueKind.Choice -> "Choice"
    is ValueKind.ListOf -> "ListOf(${element.label()})"
}
