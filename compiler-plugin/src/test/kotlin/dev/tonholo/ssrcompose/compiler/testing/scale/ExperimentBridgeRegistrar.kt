@file:OptIn(ExperimentalTopLevelDeclarationsGenerationApi::class, SymbolInternals::class)

package dev.tonholo.ssrcompose.compiler.testing.scale

import dev.tonholo.ssrcompose.catalog.options.CatalogOptionValue
import dev.tonholo.ssrcompose.compiler.producer.BridgeCatalog
import dev.tonholo.ssrcompose.compiler.producer.BridgeCatalogLoader
import dev.tonholo.ssrcompose.compiler.producer.fir.BridgeDeclarationGenerator
import dev.tonholo.ssrcompose.compiler.producer.ir.BridgeIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.copy
import org.jetbrains.kotlin.fir.declarations.utils.fileNameForPluginGeneratedCallable
import org.jetbrains.kotlin.fir.extensions.ExperimentalTopLevelDeclarationsGenerationApi
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName

private val IDENTIFIER = Regex("[A-Za-z_][A-Za-z0-9_]*")

/**
 * What the scale experiment of T031a changes in the creation of bridge functions.
 *
 * @property namedIn when set, a function is declared only for a name that appears in this source. The compiler
 * creates every function a plugin declares, so limiting what is declared is the only way to limit what is created.
 * @property files how many source files the functions of one bridge are spread over. One JVM class cannot hold the
 * functions of a full catalog.
 * @property moduleName when set, the functions are internal to the module and their classes carry this name, so two
 * modules that create the same bridge do not clash.
 */
internal data class BridgeCreation(val namedIn: String?, val files: Int, val moduleName: String? = null)

/** Registers the bridge of each catalog with the changes of [creation]. */
internal class ExperimentBridgeRegistrar(
    private val catalogs: List<CatalogOptionValue>,
    private val creation: BridgeCreation,
) : CompilerPluginRegistrar() {
    override val pluginId: String = "dev.tonholo.ssrcompose.scale.experiment"

    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        registerExperimentBridges(catalogs = catalogs, creation = creation)
    }
}

internal fun CompilerPluginRegistrar.ExtensionStorage.registerExperimentBridges(
    catalogs: List<CatalogOptionValue>,
    creation: BridgeCreation,
) {
    val namesInSource = creation.namedIn?.let { source ->
        IDENTIFIER.findAll(source).mapTo(destination = HashSet()) { it.value }
    }
    val load = BridgeCatalogLoader.load(options = catalogs)
    check(load.problems.isEmpty()) { load.problems.joinToString(separator = "\n") }
    val bridgeCatalogs = load.catalogs
    FirExtensionRegistrarAdapter.registerExtension(
        ExperimentFirRegistrar(catalogs = bridgeCatalogs, namesInSource = namesInSource, creation = creation),
    )
    IrGenerationExtension.registerExtension(BridgeIrGenerationExtension(catalogs = bridgeCatalogs))
}

private class ExperimentFirRegistrar(
    private val catalogs: List<BridgeCatalog>,
    private val namesInSource: Set<String>?,
    private val creation: BridgeCreation,
) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        catalogs.forEach { catalog ->
            +FirDeclarationGenerationExtension.Factory { session ->
                ExperimentBridgeGenerator(
                    session = session,
                    catalog = catalog,
                    namesInSource = namesInSource,
                    creation = creation,
                )
            }
        }
    }
}

private class ExperimentBridgeGenerator(
    session: FirSession,
    catalog: BridgeCatalog,
    private val namesInSource: Set<String>?,
    private val creation: BridgeCreation,
) : FirDeclarationGenerationExtension(session) {
    private val allBridges = BridgeDeclarationGenerator(session = session, catalog = catalog)

    override fun hasPackage(packageFqName: FqName): Boolean = allBridges.hasPackage(packageFqName)

    override fun getTopLevelCallableIds(): Set<CallableId> {
        val declared = allBridges.getTopLevelCallableIds()
        val names = namesInSource ?: return declared
        return declared.filterTo(destination = HashSet()) { id -> id.callableName.asString() in names }
    }

    override fun generateFunctions(
        callableId: CallableId,
        context: MemberGenerationContext?,
    ): List<FirNamedFunctionSymbol> =
        allBridges.generateFunctions(callableId = callableId, context = context).onEach { function ->
            val file = Math.floorMod(callableId.callableName.asString().hashCode(), creation.files)
            val moduleName = creation.moduleName
            function.fir.fileNameForPluginGeneratedCallable = "Bridge$file${moduleName.orEmpty()}"
            if (moduleName != null) {
                function.fir.replaceStatus(function.fir.status.copy(visibility = Visibilities.Internal))
            }
        }
}
