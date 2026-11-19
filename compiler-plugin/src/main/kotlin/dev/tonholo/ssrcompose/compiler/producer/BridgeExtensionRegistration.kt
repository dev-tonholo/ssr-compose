package dev.tonholo.ssrcompose.compiler.producer

import dev.tonholo.ssrcompose.compiler.producer.fir.BridgeFirExtensionRegistrar
import dev.tonholo.ssrcompose.compiler.producer.fir.UnplaceableBridgeCheckRegistrar
import dev.tonholo.ssrcompose.compiler.producer.ir.BridgeIrGenerationExtension
import dev.tonholo.ssrcompose.compiler.producer.scan.SourceNameScan
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.report
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

/** Creates the bridges [request] names, or reports to [messages] why they cannot be created and creates none. */
internal fun ExtensionStorage.registerBridgeExtensions(request: BridgeRequest, messages: MessageCollector) {
    registerUnplaceableBridgeCheck()
    if (request.catalogs.isEmpty()) return
    val loader = BridgeCatalogLoader(
        nameSource = SourceNameScan(roots = request.sourceRoots, excluded = request.excludedSources),
        moduleName = request.moduleName,
    )
    val load = loader.load(options = request.catalogs)
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
