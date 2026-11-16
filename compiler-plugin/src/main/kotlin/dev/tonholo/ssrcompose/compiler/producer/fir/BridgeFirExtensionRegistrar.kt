package dev.tonholo.ssrcompose.compiler.producer.fir

import dev.tonholo.ssrcompose.compiler.producer.BridgeCatalog
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

internal class BridgeFirExtensionRegistrar(
    private val catalogs: List<BridgeCatalog>,
) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        catalogs.forEach { catalog ->
            +FirDeclarationGenerationExtension.Factory { session ->
                BridgeDeclarationGenerator(session = session, catalog = catalog)
            }
        }
    }
}
