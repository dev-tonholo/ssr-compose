package dev.tonholo.ssrcompose.compiler

import dev.tonholo.ssrcompose.catalog.options.CompilerPluginBuildConfig
import dev.tonholo.ssrcompose.catalog.options.CompilerPluginMode
import dev.tonholo.ssrcompose.compiler.consumer.ConsumerFirRegistrar
import dev.tonholo.ssrcompose.compiler.producer.registerBridgeExtensions
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.report
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.MessageCollectorAccess
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter

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
        if (options.mode == CompilerPluginMode.Producer) registerBridgeExtensions(options.catalogs)
        ConsumerFirRegistrar.from(options = options, messages = configuration.messageCollector)
            ?.let { FirExtensionRegistrarAdapter.registerExtension(it) }
    }
}
