package dev.tonholo.ssrcompose.compiler.producer

import dev.tonholo.ssrcompose.catalog.bundle.CatalogFileException
import dev.tonholo.ssrcompose.catalog.index.CatalogIndex
import dev.tonholo.ssrcompose.catalog.index.CatalogIndexReader
import dev.tonholo.ssrcompose.catalog.options.CatalogOptionValue
import dev.tonholo.ssrcompose.compiler.producer.scan.ComponentNameSource
import java.io.File
import java.io.IOException
import org.jetbrains.kotlin.name.FqName

/** What reading the held catalogs gave: the catalogs, or every reason the compile cannot go on. */
internal data class BridgeCatalogLoad(
    val catalogs: List<BridgeCatalog>,
    val problems: List<String>,
)

/** Reads the held catalogs of the module called [moduleName], keeping only the components [nameSource] finds. */
internal class BridgeCatalogLoader(
    private val nameSource: ComponentNameSource,
    moduleName: String,
) {
    private val classRule = BridgeClassRule(moduleName = moduleName)
    private val namedComponents = NamedBridgeComponents(classRule = classRule)

    fun load(options: List<CatalogOptionValue>): BridgeCatalogLoad {
        val opened = options.map(::open)
        val indexes = opened.mapNotNull { it.index }
        val named = nameSource.namedAmong(names = indexes.flatMapTo(destination = HashSet()) { it.componentNames })
        val results = opened.map { result -> catalogOf(opened = result, named = named) }
        return BridgeCatalogLoad(
            catalogs = results.mapNotNull { it.catalog },
            problems = BridgeLimits.sharedPackageProblems(options = options) + results.flatMap { it.problems },
        )
    }

    private fun open(option: CatalogOptionValue): OpenedCatalog {
        val optionProblems = BridgeLimits.optionProblems(option = option)
        if (optionProblems.isNotEmpty()) return OpenedCatalog(option = option, problems = optionProblems)
        return readIndex(option = option)
    }

    private fun readIndex(option: CatalogOptionValue): OpenedCatalog =
        try {
            val index = File(option.indexPath).inputStream().use { stream -> CatalogIndexReader.read(stream = stream) }
            if (index.version.value == option.version) {
                OpenedCatalog(option = option, index = index)
            } else {
                OpenedCatalog(option = option, problems = listOf(versionMismatch(option = option, index = index)))
            }
        } catch (exception: CatalogFileException) {
            OpenedCatalog(option = option, problems = listOf(unreadable(option = option, exception = exception)))
        } catch (exception: IOException) {
            OpenedCatalog(option = option, problems = listOf(unreadable(option = option, exception = exception)))
        }

    private fun catalogOf(opened: OpenedCatalog, named: Set<String>): CatalogResult {
        val index = opened.index ?: return CatalogResult(catalog = null, problems = opened.problems)
        val option = opened.option
        return try {
            val components = namedComponents.of(index = index, named = named)
            val catalog = BridgeCatalog(
                key = BridgeKey(catalogVersion = option.version),
                bridgePackage = FqName(option.bridgePackage),
                components = components.created,
                refusals = components.refusals,
                classRule = classRule,
            )
            CatalogResult(catalog = catalog, problems = emptyList())
        } catch (exception: CatalogFileException) {
            CatalogResult(catalog = null, problems = listOf(unreadable(option = option, exception = exception)))
        }
    }

    private fun versionMismatch(option: CatalogOptionValue, index: CatalogIndex): String =
        "The catalog option names version ${option.version}, " +
            "but its index was written from version ${index.version.value}."

    private fun unreadable(option: CatalogOptionValue, exception: Exception): String =
        "The index of catalog version ${option.version} cannot be read: ${exception.message.orEmpty()}"

    private class OpenedCatalog(
        val option: CatalogOptionValue,
        val index: CatalogIndex? = null,
        val problems: List<String> = emptyList(),
    )

    private class CatalogResult(val catalog: BridgeCatalog?, val problems: List<String>)
}
