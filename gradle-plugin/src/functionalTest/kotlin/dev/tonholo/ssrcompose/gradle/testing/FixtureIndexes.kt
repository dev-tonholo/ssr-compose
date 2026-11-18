package dev.tonholo.ssrcompose.gradle.testing

import dev.tonholo.ssrcompose.catalog.index.CatalogIndexWriter
import dev.tonholo.ssrcompose.catalog.model.Catalog
import dev.tonholo.ssrcompose.catalog.model.CatalogPart
import dev.tonholo.ssrcompose.catalog.model.ChoiceList
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.catalog.model.PartName
import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
import dev.tonholo.ssrcompose.core.data.ValueKind

/** Index files for the fixture builds, written by the real index writer. */
internal object FixtureIndexes {
    private const val PART_NAME = "fixture"
    private val TITLE = Parameter.Value(name = "title", kind = ValueKind.Text, required = true)
    private val EXTRA = Parameter.Value(name = "extra", kind = ValueKind.Text, required = false, nullable = true)

    /** An index with no component, so a compile needs no producer library. [marker] makes its bytes differ. */
    fun withoutComponents(marker: String, version: String = FixtureSources.DEFAULT_VERSION): ByteArray =
        indexOf(
            part = CatalogPart(
                name = PartName(value = PART_NAME),
                choices = listOf(ChoiceList(name = marker, entries = listOf(marker))),
                components = emptyList(),
            ),
            version = version,
        )

    /** An index of [names], each with a required title. [changedComponent] has one more optional parameter. */
    fun withComponents(names: List<String>, changedComponent: String? = null): ByteArray =
        indexOf(
            part = CatalogPart(
                name = PartName(value = PART_NAME),
                choices = emptyList(),
                components = names.map { name ->
                    val extras = if (name == changedComponent) listOf(EXTRA) else emptyList()
                    ComponentDescriptor(name = name, parameters = listOf(TITLE) + extras)
                },
            ),
        )

    private fun indexOf(part: CatalogPart, version: String = FixtureSources.DEFAULT_VERSION): ByteArray =
        CatalogIndexWriter.write(catalog = Catalog(version = CatalogVersion(value = version), parts = listOf(part)))
}
