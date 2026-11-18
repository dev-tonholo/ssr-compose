package dev.tonholo.ssrcompose.compiler.producer

import dev.tonholo.ssrcompose.catalog.bundle.CatalogFileException
import dev.tonholo.ssrcompose.catalog.index.CatalogIndex
import dev.tonholo.ssrcompose.catalog.index.CatalogIndexReader
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.options.CatalogOptionValue
import java.io.File
import java.io.IOException
import org.jetbrains.kotlin.name.FqName

/** What reading the held catalogs gave: the catalogs, or every reason the compile cannot go on. */
internal data class BridgeCatalogLoad(
    val catalogs: List<BridgeCatalog>,
    val problems: List<String>,
)

internal object BridgeCatalogLoader {
    fun load(options: List<CatalogOptionValue>): BridgeCatalogLoad {
        val results = options.map(::loadCatalog)
        return BridgeCatalogLoad(
            catalogs = results.mapNotNull { it.catalog },
            problems = BridgeLimits.sharedPackageProblems(options = options) + results.flatMap { it.problems },
        )
    }

    private fun loadCatalog(option: CatalogOptionValue): CatalogResult {
        val optionProblems = BridgeLimits.optionProblems(option = option)
        if (optionProblems.isNotEmpty()) return CatalogResult(catalog = null, problems = optionProblems)
        return readCatalog(option = option)
    }

    private fun readCatalog(option: CatalogOptionValue): CatalogResult =
        try {
            val index = File(option.indexPath).inputStream().use { stream -> CatalogIndexReader.read(stream = stream) }
            if (index.version.value == option.version) {
                resultOf(option = option, index = index)
            } else {
                versionMismatch(option = option, index = index)
            }
        } catch (exception: CatalogFileException) {
            unreadable(option = option, reason = exception.message.orEmpty())
        } catch (exception: IOException) {
            unreadable(option = option, reason = exception.message.orEmpty())
        }

    private fun resultOf(option: CatalogOptionValue, index: CatalogIndex): CatalogResult {
        val descriptors = index.componentNames.mapNotNull(index::component)
        val components = descriptors.map(::bridgeComponent)
        val refused = descriptors.flatMap(::refusedParameters)
        val problems = refused + BridgeLimits.componentProblems(components = components)
        if (problems.isNotEmpty()) return CatalogResult(catalog = null, problems = problems)
        val catalog = BridgeCatalog(
            key = BridgeKey(catalogVersion = option.version),
            bridgePackage = FqName(option.bridgePackage),
            components = components,
        )
        return CatalogResult(catalog = catalog, problems = emptyList())
    }

    private fun refusedParameters(component: ComponentDescriptor): List<String> =
        component.parameters.filter { it.toBridgeParameter() == null }.map { parameter ->
            "The composable '${component.name}' cannot get a bridge: its parameter '${parameter.name}' " +
                "${parameter.shapeDescription()} is not supported yet."
        }

    private fun bridgeComponent(component: ComponentDescriptor): BridgeComponent =
        BridgeComponent(
            name = component.name,
            parameters = component.parameters.mapNotNull { it.toBridgeParameter() },
        )

    private fun versionMismatch(option: CatalogOptionValue, index: CatalogIndex): CatalogResult =
        CatalogResult(
            catalog = null,
            problems = listOf(
                "The catalog option names version ${option.version}, " +
                    "but its index was written from version ${index.version.value}.",
            ),
        )

    private fun unreadable(option: CatalogOptionValue, reason: String): CatalogResult =
        CatalogResult(
            catalog = null,
            problems = listOf("The index of catalog version ${option.version} cannot be read: $reason"),
        )

    private class CatalogResult(val catalog: BridgeCatalog?, val problems: List<String>)
}
