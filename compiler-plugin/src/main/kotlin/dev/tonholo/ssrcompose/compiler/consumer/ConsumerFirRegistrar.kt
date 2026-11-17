package dev.tonholo.ssrcompose.compiler.consumer

import dev.tonholo.ssrcompose.catalog.options.CompilerPluginMode
import dev.tonholo.ssrcompose.compiler.SsrPluginOptions
import java.io.File
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar

internal class ConsumerFirRegistrar(
    private val writer: FragmentWriter,
    private val messages: MessageCollector,
) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +FirAdditionalCheckersExtension.Factory { session ->
            ConsumerCheckersExtension(session = session, writer = writer, messages = messages)
        }
    }

    companion object {
        /** The registrar for a consumer compilation that has a fragments folder, and null for any other. */
        fun from(options: SsrPluginOptions, messages: MessageCollector): ConsumerFirRegistrar? =
            options.fragmentsDir
                ?.takeIf { options.mode == CompilerPluginMode.Consumer }
                ?.let { directory ->
                    ConsumerFirRegistrar(writer = FragmentWriter(directory = File(directory)), messages = messages)
                }
    }
}
