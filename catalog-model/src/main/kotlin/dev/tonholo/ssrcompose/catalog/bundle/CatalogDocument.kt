package dev.tonholo.ssrcompose.catalog.bundle

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys

@Serializable
internal data class CatalogDocument(
    @Contextual val format: Int,
    val version: String,
    val choices: List<ChoiceListFile>,
    val components: List<ComponentFile>,
)

@Serializable
internal data class ChoiceListFile(val name: String, val entries: List<String>)

@Serializable
internal data class ComponentFile(val name: String, val parameters: List<ParameterFile>)

@JsonIgnoreUnknownKeys
@Serializable
internal data class CatalogHeader(@Contextual val format: Int)
