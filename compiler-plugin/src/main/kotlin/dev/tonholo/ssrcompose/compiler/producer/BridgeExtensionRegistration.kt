package dev.tonholo.ssrcompose.compiler.producer

import dev.tonholo.ssrcompose.catalog.options.CatalogOptionValue
import dev.tonholo.ssrcompose.compiler.producer.fir.BridgeFirExtensionRegistrar
import dev.tonholo.ssrcompose.compiler.producer.fir.UnplaceableBridgeCheckRegistrar
import dev.tonholo.ssrcompose.compiler.producer.ir.BridgeIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

internal fun ExtensionStorage.registerBridgeExtensions(catalogs: List<CatalogOptionValue>) {
    registerUnplaceableBridgeCheck()
    if (catalogs.isEmpty()) return
    val bridgeCatalogs = catalogs.map(BridgeCatalog::from)
    FirExtensionRegistrarAdapter.registerExtension(BridgeFirExtensionRegistrar(bridgeCatalogs))
    IrGenerationExtension.registerExtension(BridgeIrGenerationExtension(bridgeCatalogs))
}

internal fun ExtensionStorage.registerUnplaceableBridgeCheck() {
    FirExtensionRegistrarAdapter.registerExtension(UnplaceableBridgeCheckRegistrar())
}
