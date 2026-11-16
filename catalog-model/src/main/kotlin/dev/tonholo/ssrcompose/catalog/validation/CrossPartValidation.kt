package dev.tonholo.ssrcompose.catalog.validation

import dev.tonholo.ssrcompose.catalog.model.CatalogPart
import dev.tonholo.ssrcompose.catalog.model.ChoiceList
import dev.tonholo.ssrcompose.catalog.model.PartName

internal fun requireDistinctPartNames(parts: List<CatalogPart>) {
    val names = HashSet<PartName>()
    parts.forEach { part ->
        require(names.add(part.name)) { "A catalog must not repeat the part name ${part.name.value}" }
    }
}

internal fun requireUniqueComponentNames(parts: List<CatalogPart>) {
    val owners = HashMap<String, PartName>()
    parts.forEach { part ->
        part.components.forEach { component ->
            val owner = owners.putIfAbsent(component.name, part.name)
            require(owner == null) {
                "The component ${component.name.cutForMessage()} is in the part ${owner?.value} " +
                    "and in the part ${part.name.value}"
            }
        }
    }
}

internal fun requireSameChoiceLists(parts: List<CatalogPart>) {
    val firsts = HashMap<String, Pair<PartName, ChoiceList>>()
    parts.forEach { part ->
        part.choices.forEach { choice ->
            val first = firsts.putIfAbsent(choice.name, part.name to choice)
            require(first == null || first.second == choice) {
                "The choice list ${choice.name.cutForMessage()} differs between the part ${first?.first?.value} " +
                    "and the part ${part.name.value}"
            }
        }
    }
}
