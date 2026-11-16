package dev.tonholo.ssrcompose.catalog.model

import dev.tonholo.ssrcompose.catalog.validation.cutForMessage
import dev.tonholo.ssrcompose.core.catalog.CatalogVersion

/**
 * Every marked composable of an app at one [version], with the choice lists its parameters use.
 *
 * The order of [choices] and [components] does not matter here: [CatalogFile] writes both sorted by name.
 *
 * There is no limit on the number of components.
 *
 * @throws IllegalArgumentException when [format] is not [CURRENT_FORMAT], a component or choice list name
 * is repeated, or a parameter names a choice list that does not exist.
 */
public data class Catalog(
    public val format: Int = CURRENT_FORMAT,
    public val version: CatalogVersion,
    public val choices: List<ChoiceList>,
    public val components: List<ComponentDescriptor>,
) {
    init {
        require(format == CURRENT_FORMAT) { "The catalog format $format is not supported" }
        require(components.map { it.name }.toSet().size == components.size) {
            "A catalog must not repeat a component name"
        }
        require(choices.map { it.name }.toSet().size == choices.size) {
            "A catalog must not repeat a choice list name"
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

    public companion object {
        public const val CURRENT_FORMAT: Int = 1
    }
}
