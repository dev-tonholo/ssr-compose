package dev.tonholo.ssrcompose.catalog.index

import dev.tonholo.ssrcompose.catalog.bundle.CatalogFileException
import dev.tonholo.ssrcompose.catalog.index.binary.BodySpan
import dev.tonholo.ssrcompose.catalog.index.binary.ComponentBodyReader
import dev.tonholo.ssrcompose.catalog.model.ChoiceList
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.core.catalog.CatalogVersion

/**
 * An opened catalog index. Opening it decodes the component names only. The parameters of a component are decoded when
 * that component is asked for.
 */
public class CatalogIndex internal constructor(
    /** The version of the catalog this index was written from. */
    public val version: CatalogVersion,
    /** Every choice list, sorted by name. */
    public val choices: List<ChoiceList>,
    private val bodySpans: Map<String, BodySpan>,
    private val bodies: ComponentBodyReader,
) {
    /** The name of every component, sorted. */
    public val componentNames: List<String> = bodySpans.keys.toList()

    /**
     * Decodes the component called [name].
     *
     * @return the component, or null when the catalog does not hold that name.
     * @throws CatalogFileException when the body of the component is damaged.
     */
    public fun component(name: String): ComponentDescriptor? {
        val span = bodySpans[name] ?: return null
        return refusingDamage { bodies.read(name = name, span = span) }
    }
}
