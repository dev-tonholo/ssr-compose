package dev.tonholo.ssrcompose.catalog.model

import dev.tonholo.ssrcompose.core.data.ValueKind

internal fun ValueKind.choiceListNames(): Set<String> = when (this) {
    is ValueKind.Choice -> setOf(listName)
    is ValueKind.ListOf -> element.choiceListNames()
    else -> emptySet()
}
