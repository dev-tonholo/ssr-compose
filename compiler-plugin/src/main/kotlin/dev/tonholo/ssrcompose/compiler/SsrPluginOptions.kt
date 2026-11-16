package dev.tonholo.ssrcompose.compiler

import dev.tonholo.ssrcompose.catalog.options.CatalogOptionValue
import dev.tonholo.ssrcompose.catalog.options.CompilerPluginMode
import org.jetbrains.kotlin.config.CompilerConfiguration

internal data class SsrPluginOptions(
    val mode: CompilerPluginMode,
    val fragmentsDir: String?,
    val catalogs: List<CatalogOptionValue>,
) {
    companion object {
        fun from(configuration: CompilerConfiguration): SsrPluginOptions? {
            val mode = configuration[SsrConfigurationKeys.MODE] ?: return null
            return SsrPluginOptions(
                mode = mode,
                fragmentsDir = configuration[SsrConfigurationKeys.FRAGMENTS_DIR],
                catalogs = configuration[SsrConfigurationKeys.CATALOGS].orEmpty(),
            )
        }
    }
}
