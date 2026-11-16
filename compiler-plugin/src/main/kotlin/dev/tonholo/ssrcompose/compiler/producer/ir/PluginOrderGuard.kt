@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package dev.tonholo.ssrcompose.compiler.producer.ir

import dev.tonholo.ssrcompose.catalog.options.CompilerPluginBuildConfig
import dev.tonholo.ssrcompose.compiler.producer.BridgeKey
import dev.tonholo.ssrcompose.compiler.producer.ProducerNames
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI

/**
 * Stops the build when this plugin's IR step runs after the Compose compiler's. In that order a created function
 * already has the Compose compiler's extra parameters, and the bridge would be built wrongly without any error.
 */
internal object PluginOrderGuard {
    private const val COMPOSER_PARAMETER = "\$composer"

    val message: String =
        "The bridge plugin '${CompilerPluginBuildConfig.COMPILER_PLUGIN_ID}' must run before the Compose compiler " +
            "plugin. Pass -Xcompiler-plugin-order=${CompilerPluginBuildConfig.COMPILER_PLUGIN_ID}>" +
            ProducerNames.COMPOSE_PLUGIN_ID

    fun isViolatedBy(module: IrModuleFragment): Boolean =
        module.files
            .flatMap { it.declarations }
            .filterIsInstance<IrSimpleFunction>()
            .filter { it.isCreatedByBridge() }
            .any { function -> function.parameters.any { it.name.asString() == COMPOSER_PARAMETER } }

    private fun IrDeclaration.isCreatedByBridge(): Boolean =
        (origin as? IrDeclarationOrigin.GeneratedByPlugin)?.pluginKey is BridgeKey
}
