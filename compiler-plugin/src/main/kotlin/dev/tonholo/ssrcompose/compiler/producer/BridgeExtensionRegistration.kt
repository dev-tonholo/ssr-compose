package dev.tonholo.ssrcompose.compiler.producer

import dev.tonholo.ssrcompose.catalog.options.CatalogOptionValue
import dev.tonholo.ssrcompose.compiler.producer.fir.BridgeFirExtensionRegistrar
import dev.tonholo.ssrcompose.compiler.producer.fir.UnplaceableBridgeCheckRegistrar
import dev.tonholo.ssrcompose.compiler.producer.ir.BridgeIrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.report
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

/** Creates the bridges of [catalogs], or reports to [messages] why they cannot be created and creates none. */
internal fun ExtensionStorage.registerBridgeExtensions(catalogs: List<CatalogOptionValue>, messages: MessageCollector) {
    registerUnplaceableBridgeCheck()
    if (catalogs.isEmpty()) return
    val load = BridgeCatalogLoader.load(options = catalogs)
    if (load.problems.isNotEmpty()) {
        load.problems.forEach { problem ->
            messages.report(severity = CompilerMessageSeverity.ERROR, message = problem)
        }
        return
    }
    FirExtensionRegistrarAdapter.registerExtension(BridgeFirExtensionRegistrar(load.catalogs))
    IrGenerationExtension.registerExtension(BridgeIrGenerationExtension(load.catalogs))
}

internal fun ExtensionStorage.registerUnplaceableBridgeCheck() {
    FirExtensionRegistrarAdapter.registerExtension(UnplaceableBridgeCheckRegistrar())
}
