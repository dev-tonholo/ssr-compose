@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package dev.tonholo.ssrcompose.compiler.ir

import dev.tonholo.ssrcompose.catalog.options.CompilerPluginBuildConfig
import kotlin.reflect.KClass
import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI

/**
 * Stops the build when this plugin's IR step runs after the Compose compiler's. In that order a created function
 * already has the Compose compiler's extra parameters, and the bridge or renderer would be built wrongly without
 * any error.
 */
internal object PluginOrderGuard {
    private const val COMPOSE_PLUGIN_ID = "androidx.compose.compiler.plugins.kotlin"
    private const val COMPOSER_PARAMETER = "\$composer"

    val message: String =
        "The SSR Compose compiler plugin '${CompilerPluginBuildConfig.COMPILER_PLUGIN_ID}' must run before the " +
            "Compose compiler plugin. Pass -Xcompiler-plugin-order=${CompilerPluginBuildConfig.COMPILER_PLUGIN_ID}>" +
            COMPOSE_PLUGIN_ID

    /**
     * Reports the plugin-order diagnostic and returns true when the Compose compiler ran before this plugin on a
     * function created with [keyClass].
     */
    fun reportIfViolated(
        module: IrModuleFragment,
        context: IrPluginContext,
        keyClass: KClass<out GeneratedDeclarationKey>,
    ): Boolean {
        val violated = isViolatedBy(module = module, keyClass = keyClass)
        if (violated) {
            context.diagnosticReporter.report(
                factory = PluginOrderDiagnostics.PLUGIN_ORDER,
                message = message,
                location = null,
            )
        }
        return violated
    }

    fun isViolatedBy(module: IrModuleFragment, keyClass: KClass<out GeneratedDeclarationKey>): Boolean =
        module.files
            .flatMap { file -> file.declarations }
            .filterIsInstance<IrSimpleFunction>()
            .filter { function -> function.isCreatedBy(keyClass = keyClass) }
            .any { function ->
                function.parameters.any { parameter -> parameter.name.asString() == COMPOSER_PARAMETER }
            }

    private fun IrDeclaration.isCreatedBy(keyClass: KClass<out GeneratedDeclarationKey>): Boolean {
        val key = (origin as? IrDeclarationOrigin.GeneratedByPlugin)?.pluginKey
        return keyClass.isInstance(key)
    }
}
