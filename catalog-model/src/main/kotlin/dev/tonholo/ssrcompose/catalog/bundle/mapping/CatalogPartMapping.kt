package dev.tonholo.ssrcompose.catalog.bundle.mapping

import dev.tonholo.ssrcompose.catalog.bundle.serialization.CatalogFormat
import dev.tonholo.ssrcompose.catalog.bundle.serialization.file.CatalogPartFile
import dev.tonholo.ssrcompose.catalog.bundle.serialization.file.ChoiceListFile
import dev.tonholo.ssrcompose.catalog.bundle.serialization.file.ComponentFile
import dev.tonholo.ssrcompose.catalog.model.CatalogPart
import dev.tonholo.ssrcompose.catalog.model.ChoiceList
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.PartName

internal fun CatalogPart.toPartFile(): CatalogPartFile = CatalogPartFile(
    format = CatalogFormat.CURRENT,
    part = name.value,
    choices = choices.sortedBy { it.name }.map(ChoiceList::toFile),
    components = components.sortedBy { it.name }.map(ComponentDescriptor::toFile),
)

internal fun CatalogPartFile.toPart(): CatalogPart = CatalogPart(
    name = PartName(value = part),
    choices = choices.map(ChoiceListFile::toModel),
    components = components.map(ComponentFile::toModel),
)

private fun ChoiceList.toFile(): ChoiceListFile = ChoiceListFile(name = name, entries = entries)

private fun ChoiceListFile.toModel(): ChoiceList = ChoiceList(name = name, entries = entries)

internal fun ComponentDescriptor.toFile(): ComponentFile =
    ComponentFile(name = name, parameters = parameters.map { it.toFile() })

internal fun ComponentFile.toModel(): ComponentDescriptor =
    ComponentDescriptor(name = name, parameters = parameters.map { it.toModel() })
