package dev.tonholo.ssrcompose.compiler.consumer

import dev.tonholo.ssrcompose.catalog.options.CompilerPluginMode
import dev.tonholo.ssrcompose.compiler.SsrPluginOptions
import dev.tonholo.ssrcompose.compiler.consumer.renderer.fir.RendererDeclarationGenerator
import dev.tonholo.ssrcompose.compiler.consumer.renderer.fir.checker.RendererCheckersExtension
import java.io.File
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

internal class ConsumerFirRegistrar(
    private val writer: FragmentWriter?,
    private val messages: MessageCollector,
) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::RendererDeclarationGenerator
        +::RendererCheckersExtension
        if (writer != null) {
            +FirAdditionalCheckersExtension.Factory { session ->
                ConsumerCheckersExtension(session = session, writer = writer, messages = messages)
            }
        }
    }

    companion object {
        /** The registrar for a consumer compilation, and null for any other mode. */
        fun from(options: SsrPluginOptions, messages: MessageCollector): ConsumerFirRegistrar? {
            if (options.mode != CompilerPluginMode.Consumer) return null
            val writer = options.fragmentsDir?.let { directory -> FragmentWriter(directory = File(directory)) }
            return ConsumerFirRegistrar(writer = writer, messages = messages)
        }
    }
}
