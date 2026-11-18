@file:OptIn(ExperimentalTopLevelDeclarationsGenerationApi::class, SymbolInternals::class)

package dev.tonholo.ssrcompose.compiler.testing.scale

import dev.tonholo.ssrcompose.catalog.options.CatalogOptionValue
import dev.tonholo.ssrcompose.compiler.producer.BridgeCatalog
import dev.tonholo.ssrcompose.compiler.producer.fir.BridgeDeclarationGenerator
import dev.tonholo.ssrcompose.compiler.producer.ir.BridgeIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirSession
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
 */
internal data class BridgeCreation(val namedIn: String?, val files: Int)

/** Registers the bridge of each catalog with the changes of [creation]. */
internal class ExperimentBridgeRegistrar(
    private val catalogs: List<CatalogOptionValue>,
    private val creation: BridgeCreation,
) : CompilerPluginRegistrar() {
    override val pluginId: String = "dev.tonholo.ssrcompose.scale.experiment"

    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val namesInSource = creation.namedIn?.let { source ->
            IDENTIFIER.findAll(source).mapTo(destination = HashSet()) { it.value }
        }
        val bridgeCatalogs = catalogs.map(BridgeCatalog::from)
        FirExtensionRegistrarAdapter.registerExtension(
            ExperimentFirRegistrar(catalogs = bridgeCatalogs, namesInSource = namesInSource, files = creation.files),
        )
        IrGenerationExtension.registerExtension(BridgeIrGenerationExtension(bridgeCatalogs))
    }
}

private class ExperimentFirRegistrar(
    private val catalogs: List<BridgeCatalog>,
    private val namesInSource: Set<String>?,
    private val files: Int,
) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        catalogs.forEach { catalog ->
            +FirDeclarationGenerationExtension.Factory { session ->
                ExperimentBridgeGenerator(
                    session = session,
                    catalog = catalog,
                    namesInSource = namesInSource,
                    files = files,
                )
            }
        }
    }
}

private class ExperimentBridgeGenerator(
    session: FirSession,
    catalog: BridgeCatalog,
    private val namesInSource: Set<String>?,
    private val files: Int,
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
            val file = Math.floorMod(callableId.callableName.asString().hashCode(), files)
            function.fir.fileNameForPluginGeneratedCallable = "Bridge$file"
        }
}
