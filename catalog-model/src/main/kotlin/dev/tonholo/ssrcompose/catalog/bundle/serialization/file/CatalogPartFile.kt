package dev.tonholo.ssrcompose.catalog.bundle.serialization.file

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
internal data class CatalogPartFile(
    @Contextual val format: Int,
    val part: String,
    val choices: List<ChoiceListFile>,
    val components: List<ComponentFile>,
)

@Serializable
internal data class ChoiceListFile(val name: String, val entries: List<String>)

@Serializable
internal data class ComponentFile(val name: String, val parameters: List<ParameterFile>)
