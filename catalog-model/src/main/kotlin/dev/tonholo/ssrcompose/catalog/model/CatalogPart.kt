package dev.tonholo.ssrcompose.catalog.model

import dev.tonholo.ssrcompose.catalog.validation.cutForMessage

/**
 * The marked composables of one app module, with the choice lists their parameters use. It stands on its own: it
 * lists every choice list its components use, even when another part lists the same one.
 *
 * The order of [choices] and [components] does not matter here: the part file has both sorted by name.
 *
 * There is no limit on the number of components.
 *
 * @throws IllegalArgumentException when a component or choice list name is repeated, or a parameter names a
 * choice list that does not exist.
 */
public data class CatalogPart(
    public val name: PartName,
    public val choices: List<ChoiceList>,
    public val components: List<ComponentDescriptor>,
) {
    init {
        require(components.map { it.name }.toSet().size == components.size) {
            "A part must not repeat a component name"
        }
        require(choices.map { it.name }.toSet().size == choices.size) {
            "A part must not repeat a choice list name"
        }
        requireKnownChoiceLists()
    }

    private fun requireKnownChoiceLists() {
        val known = choices.mapTo(destination = mutableSetOf()) { it.name }
        components.forEach { component ->
            val unknown = component.parameters.flatMapTo(destination = mutableSetOf()) { it.choiceListNames() } - known
            require(unknown.isEmpty()) {
                "The component ${component.name} names a choice list that does not exist: " +
                    unknown.first().cutForMessage()
            }
        }
    }
}
