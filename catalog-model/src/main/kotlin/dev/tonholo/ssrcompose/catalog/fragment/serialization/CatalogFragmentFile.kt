package dev.tonholo.ssrcompose.catalog.fragment.serialization

import dev.tonholo.ssrcompose.catalog.bundle.serialization.file.ChoiceListFile
import dev.tonholo.ssrcompose.catalog.bundle.serialization.file.ComponentFile
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
internal data class CatalogFragmentFile(
    @Contextual val format: Int,
    val source: String,
    val choices: List<ChoiceListFile> = emptyList(),
    val components: List<ComponentFile>,
)
