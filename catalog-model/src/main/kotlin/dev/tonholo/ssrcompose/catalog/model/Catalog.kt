package dev.tonholo.ssrcompose.catalog.model

import dev.tonholo.ssrcompose.catalog.validation.requireDistinctPartNames
import dev.tonholo.ssrcompose.catalog.validation.requireSameChoiceLists
import dev.tonholo.ssrcompose.catalog.validation.requireUniqueComponentNames
import dev.tonholo.ssrcompose.core.catalog.CatalogVersion

/**
 * Every marked composable of an app at one [version], in the parts of its modules. The order of [parts] does not
 * matter here: the manifest has them sorted by name.
 *
 * There is no limit on the number of parts, components or parameters.
 *
 * @throws IllegalArgumentException when a part name is repeated, a component name is used in two parts, or a
 * choice list of one part differs from the choice list of another part with the same name. The message names
 * both parts.
 */
public data class Catalog(public val version: CatalogVersion, public val parts: List<CatalogPart>) {
    init {
        requireDistinctPartNames(parts = parts)
        requireUniqueComponentNames(parts = parts)
        requireSameChoiceLists(parts = parts)
    }
}
