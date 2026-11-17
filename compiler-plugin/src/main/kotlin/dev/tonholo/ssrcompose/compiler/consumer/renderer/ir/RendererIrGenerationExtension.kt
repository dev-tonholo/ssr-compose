package dev.tonholo.ssrcompose.compiler.consumer.renderer.ir

import dev.tonholo.ssrcompose.compiler.consumer.renderer.RendererKey
import dev.tonholo.ssrcompose.compiler.ir.PluginOrderGuard
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/**
 * Fills the IR bodies of created consumer renderers, stopping first if the Compose compiler already ran.
 */
internal class RendererIrGenerationExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val isViolated = PluginOrderGuard.reportIfViolated(
            module = moduleFragment,
            context = pluginContext,
            keyClass = RendererKey::class,
        )
        if (isViolated) return
        moduleFragment.transformChildrenVoid(
            transformer = RendererBodyGenerator(module = moduleFragment, context = pluginContext),
        )
    }
}
