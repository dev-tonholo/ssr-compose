package dev.tonholo.ssrcompose.catalog.model

import dev.tonholo.ssrcompose.catalog.validation.requireIdentifier

/**
 * A fixed list of choices a parameter can use, in declaration order.
 *
 * @throws IllegalArgumentException when a name is not a Kotlin identifier, or there is no entry or a repeated one.
 */
public data class ChoiceList(public val name: String, public val entries: List<String>) {
    init {
        requireIdentifier(text = name, what = "A choice list name")
        require(entries.isNotEmpty()) { "The choice list $name must have at least one entry" }
        entries.forEach { entry -> requireIdentifier(text = entry, what = "An entry of the choice list $name") }
        require(entries.distinct().size == entries.size) { "The choice list $name must not repeat an entry" }
    }
}
