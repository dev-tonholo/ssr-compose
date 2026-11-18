@file:OptIn(ExperimentalTopLevelDeclarationsGenerationApi::class)

package dev.tonholo.ssrcompose.compiler.testing.scale

import dev.tonholo.ssrcompose.catalog.options.CatalogOptionValue
import dev.tonholo.ssrcompose.compiler.producer.BridgeCatalog
import dev.tonholo.ssrcompose.compiler.producer.fir.BridgeDeclarationGenerator
import dev.tonholo.ssrcompose.compiler.producer.ir.BridgeIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.ExperimentalTopLevelDeclarationsGenerationApi
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName

private val IDENTIFIER = Regex("[A-Za-z_][A-Za-z0-9_]*")

/**
 * The experiment of T031a: it declares a bridge function only for a name that appears in [source]. The compiler
 * creates every function a plugin declares, so limiting what is declared is the only way to limit what is created.
 */
internal class NamedOnlyBridgeRegistrar(
    private val catalogs: List<CatalogOptionValue>,
    private val source: String,
) : CompilerPluginRegistrar() {
    override val pluginId: String = "dev.tonholo.ssrcompose.scale.named-only"

    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val namesInSource = IDENTIFIER.findAll(source).mapTo(destination = HashSet()) { it.value }
        val bridgeCatalogs = catalogs.map(BridgeCatalog::from)
        FirExtensionRegistrarAdapter.registerExtension(
            NamedOnlyFirRegistrar(catalogs = bridgeCatalogs, namesInSource = namesInSource),
        )
        IrGenerationExtension.registerExtension(BridgeIrGenerationExtension(bridgeCatalogs))
    }
}

private class NamedOnlyFirRegistrar(
    private val catalogs: List<BridgeCatalog>,
    private val namesInSource: Set<String>,
) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        catalogs.forEach { catalog ->
            +FirDeclarationGenerationExtension.Factory { session ->
                NamedOnlyBridgeGenerator(session = session, catalog = catalog, namesInSource = namesInSource)
            }
        }
    }
}

private class NamedOnlyBridgeGenerator(
    session: FirSession,
    catalog: BridgeCatalog,
    private val namesInSource: Set<String>,
) : FirDeclarationGenerationExtension(session) {
    private val allBridges = BridgeDeclarationGenerator(session = session, catalog = catalog)

    override fun hasPackage(packageFqName: FqName): Boolean = allBridges.hasPackage(packageFqName)

    override fun getTopLevelCallableIds(): Set<CallableId> =
        allBridges.getTopLevelCallableIds().filterTo(destination = HashSet()) { id ->
            id.callableName.asString() in namesInSource
        }

    override fun generateFunctions(
        callableId: CallableId,
        context: MemberGenerationContext?,
    ): List<FirNamedFunctionSymbol> = allBridges.generateFunctions(callableId = callableId, context = context)
}
