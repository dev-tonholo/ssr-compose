package dev.tonholo.ssrcompose.compiler

import dev.tonholo.ssrcompose.catalog.options.CompilerPluginBuildConfig
import dev.tonholo.ssrcompose.catalog.options.CompilerPluginMode
import dev.tonholo.ssrcompose.compiler.consumer.ConsumerFirRegistrar
import dev.tonholo.ssrcompose.compiler.consumer.renderer.ir.RendererIrGenerationExtension
import dev.tonholo.ssrcompose.compiler.producer.BridgeRequest
import dev.tonholo.ssrcompose.compiler.producer.registerBridgeExtensions
import dev.tonholo.ssrcompose.compiler.producer.registerUnplaceableBridgeCheck
import dev.tonholo.ssrcompose.compiler.producer.scan.ExcludedSources
import dev.tonholo.ssrcompose.compiler.producer.scan.ScanRoots
import java.io.File
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.config.kotlinSourceRoots
import org.jetbrains.kotlin.cli.common.messages.report
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CommonConfigurationKeys
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
        if (options.mode == CompilerPluginMode.Producer) {
            registerBridgeExtensions(
                request = bridgeRequest(options = options, configuration = configuration),
                messages = configuration.messageCollector,
            )
        }
        if (options.mode == CompilerPluginMode.Check) registerUnplaceableBridgeCheck()
        ConsumerFirRegistrar.from(options = options, messages = configuration.messageCollector)?.let { registrar ->
            FirExtensionRegistrarAdapter.registerExtension(registrar)
            IrGenerationExtension.registerExtension(RendererIrGenerationExtension())
        }
    }

    private fun bridgeRequest(options: SsrPluginOptions, configuration: CompilerConfiguration): BridgeRequest =
        BridgeRequest(
            catalogs = options.catalogs,
            sourceRoots = sourceRootsOf(options = options, configuration = configuration),
            excludedSources = ExcludedSources.read(
                listFile = options.excludedSources?.let(::File),
                compilerRoots = configuration.kotlinSourceRoots.map { root -> File(root.path) },
            ),
            moduleName = configuration[CommonConfigurationKeys.MODULE_NAME] ?: DEFAULT_MODULE_NAME,
        )

    private fun sourceRootsOf(options: SsrPluginOptions, configuration: CompilerConfiguration): List<File> =
        ScanRoots.of(
            sourceFolders = options.sourceFolders.map(::File),
            compilerRoots = configuration.kotlinSourceRoots.map { root -> File(root.path) },
        )

    private companion object {
        const val DEFAULT_MODULE_NAME = "main"
    }
}
