package dev.tonholo.ssrcompose.compiler.producer.text

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar.ExtensionStorage
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

/** Puts the producer's `ScaledBasicText` in place of each call to `BasicText`. It needs no catalog. */
internal fun ExtensionStorage.registerTextRewrite() {
    IrGenerationExtension.registerExtension(TextRewriteIrExtension())
}

private class TextRewriteIrExtension : IrGenerationExtension {
    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val overloads = ScaledTextOverloads(context = pluginContext)
        if (!overloads.isAvailable) return
        moduleFragment.transformChildrenVoid(
            transformer = TextCallRewriter(context = pluginContext, overloads = overloads),
        )
    }
}
