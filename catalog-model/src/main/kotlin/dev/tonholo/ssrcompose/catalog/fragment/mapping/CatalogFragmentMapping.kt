package dev.tonholo.ssrcompose.catalog.fragment.mapping

import dev.tonholo.ssrcompose.catalog.bundle.mapping.toFile
import dev.tonholo.ssrcompose.catalog.bundle.mapping.toModel
import dev.tonholo.ssrcompose.catalog.bundle.serialization.CatalogFormat
import dev.tonholo.ssrcompose.catalog.bundle.serialization.file.ChoiceListFile
import dev.tonholo.ssrcompose.catalog.bundle.serialization.file.ComponentFile
import dev.tonholo.ssrcompose.catalog.fragment.CatalogFragment
import dev.tonholo.ssrcompose.catalog.fragment.serialization.CatalogFragmentFile
import dev.tonholo.ssrcompose.catalog.model.ChoiceList
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor

internal fun CatalogFragment.toFragmentFile(): CatalogFragmentFile = CatalogFragmentFile(
    format = CatalogFormat.CURRENT,
    source = source,
    choices = choices.sortedBy { it.name }.map(ChoiceList::toFile),
    components = components.sortedBy { it.name }.map(ComponentDescriptor::toFile),
)

internal fun CatalogFragmentFile.toFragment(): CatalogFragment = CatalogFragment(
    source = source,
    choices = choices.map(ChoiceListFile::toModel),
    components = components.map(ComponentFile::toModel),
)
