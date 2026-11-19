package dev.tonholo.ssrcompose.compiler.testing

import dev.tonholo.ssrcompose.catalog.model.Catalog
import dev.tonholo.ssrcompose.catalog.options.CatalogOptionValue
import dev.tonholo.ssrcompose.catalog.options.CompilerPluginMode
import dev.tonholo.ssrcompose.catalog.options.CompilerPluginOptions
import java.io.File

internal object ProducerTestOptions {
    /** The option that holds [catalog]. A test of a version mismatch sets [version] to another value. */
    fun catalog(
        catalog: Catalog,
        bridgePackage: String,
        version: String = catalog.version.value,
    ): CatalogOptionValue =
        CatalogOptionValue(
            version = version,
            indexPath = TestCatalogs.indexPath(catalog = catalog),
            bridgePackage = bridgePackage,
        )

    fun checkOptions(): List<Pair<String, String>> =
        listOf(CompilerPluginOptions.MODE to CompilerPluginMode.Check.optionValue)

    fun sourceFolderOption(folder: File): Pair<String, String> =
        CompilerPluginOptions.SOURCE_FOLDER to folder.absolutePath

    fun producerOptions(vararg catalogs: CatalogOptionValue): List<Pair<String, String>> =
        listOf(CompilerPluginOptions.MODE to CompilerPluginMode.Producer.optionValue) +
            catalogs.map { CompilerPluginOptions.CATALOG to it.encode() }
}
