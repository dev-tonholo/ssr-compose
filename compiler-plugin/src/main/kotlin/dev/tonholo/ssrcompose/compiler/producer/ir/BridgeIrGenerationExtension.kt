package dev.tonholo.ssrcompose.compiler.producer.ir

import dev.tonholo.ssrcompose.compiler.producer.BridgeCatalog
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

internal class BridgeIrGenerationExtension(
    private val catalogs: List<BridgeCatalog>,
) : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        moduleFragment.transformChildrenVoid(BridgeBodyGenerator(catalogs, pluginContext))
    }
}
