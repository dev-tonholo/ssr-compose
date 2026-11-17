package dev.tonholo.ssrcompose.compiler.testing

import dev.tonholo.ssrcompose.catalog.options.CatalogOptionValue
import dev.tonholo.ssrcompose.catalog.options.CompilerPluginMode
import dev.tonholo.ssrcompose.catalog.options.CompilerPluginOptions

internal object ProducerTestOptions {
    fun catalog(indexName: String, version: String, bridgePackage: String): CatalogOptionValue =
        CatalogOptionValue(
            version = version,
            indexPath = ThrowawayIndexFiles.path(indexName = indexName),
            bridgePackage = bridgePackage,
        )

    fun checkOptions(): List<Pair<String, String>> =
        listOf(CompilerPluginOptions.MODE to CompilerPluginMode.Check.optionValue)

    fun producerOptions(vararg catalogs: CatalogOptionValue): List<Pair<String, String>> =
        listOf(CompilerPluginOptions.MODE to CompilerPluginMode.Producer.optionValue) +
            catalogs.map { CompilerPluginOptions.CATALOG to it.encode() }
}
