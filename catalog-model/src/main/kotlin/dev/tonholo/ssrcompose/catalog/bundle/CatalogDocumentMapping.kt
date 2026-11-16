package dev.tonholo.ssrcompose.catalog.bundle

import dev.tonholo.ssrcompose.catalog.model.Catalog
import dev.tonholo.ssrcompose.catalog.model.ChoiceList
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.core.catalog.CatalogVersion

internal fun Catalog.toDocument(): CatalogDocument = CatalogDocument(
    format = format,
    version = version.value,
    choices = choices.sortedBy { it.name }.map(ChoiceList::toFile),
    components = components.sortedBy { it.name }.map(ComponentDescriptor::toFile),
)

internal fun CatalogDocument.toCatalog(): Catalog = Catalog(
    format = format,
    version = CatalogVersion(value = version),
    choices = choices.map(ChoiceListFile::toModel),
    components = components.map(ComponentFile::toModel),
)

private fun ChoiceList.toFile(): ChoiceListFile = ChoiceListFile(name = name, entries = entries)

private fun ChoiceListFile.toModel(): ChoiceList = ChoiceList(name = name, entries = entries)

private fun ComponentDescriptor.toFile(): ComponentFile =
    ComponentFile(name = name, parameters = parameters.map { it.toFile() })

private fun ComponentFile.toModel(): ComponentDescriptor =
    ComponentDescriptor(name = name, parameters = parameters.map { it.toModel() })
