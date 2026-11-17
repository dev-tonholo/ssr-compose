package dev.tonholo.ssrcompose.compiler.producer.ir

import dev.tonholo.ssrcompose.compiler.ir.PluginOrderGuard
import dev.tonholo.ssrcompose.compiler.producer.BridgeCatalog
import dev.tonholo.ssrcompose.compiler.producer.BridgeKey
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

internal class BridgeIrGenerationExtension(
    private val catalogs: List<BridgeCatalog>,
) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val isViolated = PluginOrderGuard.reportIfViolated(
            module = moduleFragment,
            context = pluginContext,
            keyClass = BridgeKey::class,
        )
        if (isViolated) return
        moduleFragment.transformChildrenVoid(
            transformer = BridgeBodyGenerator(catalogs = catalogs, context = pluginContext),
        )
    }
}
