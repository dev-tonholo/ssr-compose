package dev.tonholo.ssrcompose.compiler.ir

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.KtSourcelessDiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.SourcelessDiagnosticFactoryDelegateProvider
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.BaseSourcelessDiagnosticRendererFactory

/**
 * Compiler diagnostic reported when the Compose compiler plugin runs before this plugin's IR step.
 */
internal object PluginOrderDiagnostics : KtDiagnosticsContainer() {
    val PLUGIN_ORDER: KtSourcelessDiagnosticFactory by SourcelessDiagnosticFactoryDelegateProvider(
        severity = Severity.ERROR,
        container = this,
    )

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = PluginOrderDiagnosticRenderers
}

internal object PluginOrderDiagnosticRenderers : BaseSourcelessDiagnosticRendererFactory() {
    override val MAP: KtDiagnosticFactoryToRendererMap by
        KtDiagnosticFactoryToRendererMap("PluginOrderDiagnostics") { map ->
            map.put(factory = PluginOrderDiagnostics.PLUGIN_ORDER, message = MESSAGE_PLACEHOLDER)
        }
}
