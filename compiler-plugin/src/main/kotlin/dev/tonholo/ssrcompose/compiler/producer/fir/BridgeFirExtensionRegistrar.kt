package dev.tonholo.ssrcompose.compiler.producer.fir

import dev.tonholo.ssrcompose.compiler.producer.BridgeCatalog
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.Name

internal class BridgeFirExtensionRegistrar(
    private val catalogs: List<BridgeCatalog>,
) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        catalogs.forEach { catalog ->
            +FirDeclarationGenerationExtension.Factory { session ->
                BridgeDeclarationGenerator(session = session, catalog = catalog)
            }
        }
        val refusals = catalogs.flatMap { catalog ->
            catalog.refusals.map { (name, refusal) ->
                CallableId(catalog.bridgePackage, Name.identifier(name)) to refusal
            }
        }.toMap()
        if (refusals.isNotEmpty()) {
            +FirAdditionalCheckersExtension.Factory { session ->
                RefusedBridgeCheckers(session = session, refusals = refusals)
            }
        }
    }
}
