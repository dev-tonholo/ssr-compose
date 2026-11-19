package dev.tonholo.ssrcompose.compiler

import dev.tonholo.ssrcompose.catalog.options.CatalogOptionValue
import dev.tonholo.ssrcompose.catalog.options.CompilerPluginMode
import dev.tonholo.ssrcompose.catalog.options.CompilerPluginOptions
import org.jetbrains.kotlin.config.CompilerConfigurationKey

internal object SsrConfigurationKeys {
    val MODE: CompilerConfigurationKey<CompilerPluginMode> =
        CompilerConfigurationKey.create(CompilerPluginOptions.MODE)
    val FRAGMENTS_DIR: CompilerConfigurationKey<String> =
        CompilerConfigurationKey.create(CompilerPluginOptions.FRAGMENTS_DIR)
    val CATALOGS: CompilerConfigurationKey<MutableList<CatalogOptionValue>> =
        CompilerConfigurationKey.create(CompilerPluginOptions.CATALOG)
    val SOURCE_FOLDERS: CompilerConfigurationKey<MutableList<String>> =
        CompilerConfigurationKey.create(CompilerPluginOptions.SOURCE_FOLDER)
    val EXCLUDED_SOURCES: CompilerConfigurationKey<String> =
        CompilerConfigurationKey.create(CompilerPluginOptions.EXCLUDED_SOURCES)
}
