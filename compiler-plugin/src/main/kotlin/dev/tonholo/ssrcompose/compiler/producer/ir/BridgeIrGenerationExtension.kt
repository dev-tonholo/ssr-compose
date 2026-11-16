package dev.tonholo.ssrcompose.compiler.producer.ir

import dev.tonholo.ssrcompose.compiler.producer.BridgeCatalog
import dev.tonholo.ssrcompose.compiler.producer.diagnostic.BridgeDiagnostics
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

internal class BridgeIrGenerationExtension(
    private val catalogs: List<BridgeCatalog>,
) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        if (PluginOrderGuard.isViolatedBy(moduleFragment)) {
            pluginContext.diagnosticReporter.report(
                factory = BridgeDiagnostics.PLUGIN_ORDER,
                message = PluginOrderGuard.message,
                location = null,
            )
            return
        }
        moduleFragment.transformChildrenVoid(BridgeBodyGenerator(catalogs = catalogs, context = pluginContext))
    }
}
