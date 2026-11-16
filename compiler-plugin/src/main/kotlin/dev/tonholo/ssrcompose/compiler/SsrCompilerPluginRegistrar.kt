package dev.tonholo.ssrcompose.compiler

import dev.tonholo.ssrcompose.catalog.options.CompilerPluginBuildConfig
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.report
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.MessageCollectorAccess
import org.jetbrains.kotlin.config.messageCollector

internal class SsrCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val pluginId: String = CompilerPluginBuildConfig.COMPILER_PLUGIN_ID

    override val supportsK2: Boolean = true

    @OptIn(MessageCollectorAccess::class)
    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val options = SsrPluginOptions.from(configuration) ?: return
        configuration.messageCollector.report(
            severity = CompilerMessageSeverity.INFO,
            message = "SSR Compose compiler plugin received $options",
        )
    }
}
