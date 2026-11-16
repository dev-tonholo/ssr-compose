package dev.tonholo.ssrcompose.gradle.testing

import dev.tonholo.ssrcompose.catalog.options.CatalogOptionValue
import dev.tonholo.ssrcompose.catalog.options.CompilerPluginBuildConfig
import dev.tonholo.ssrcompose.catalog.options.CompilerPluginOptions

internal data class CompilerSetup(
    val pluginOptions: List<String>,
    val freeCompilerArguments: List<String>,
    val inputProperties: List<String>,
    val pluginDependencies: List<String>,
    val implementationDependencies: List<String>,
) {
    fun optionValue(optionName: String): String? =
        optionValues(optionName).firstOrNull()

    fun optionValues(optionName: String): List<String> {
        val prefix = "plugin:${CompilerPluginBuildConfig.COMPILER_PLUGIN_ID}:$optionName="
        return pluginOptions.filter { it.startsWith(prefix) }.map { it.removePrefix(prefix) }
    }

    fun catalogOptions(): List<CatalogOptionValue> =
        optionValues(CompilerPluginOptions.CATALOG).map { CatalogOptionValue.decode(it) }
}
