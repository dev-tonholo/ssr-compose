package dev.tonholo.ssrcompose.compiler.producer.diagnostic

import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.KtSourcelessDiagnosticFactory
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.SourcelessDiagnosticFactoryDelegateProvider
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.BaseSourcelessDiagnosticRendererFactory

internal object BridgeDiagnostics : KtDiagnosticsContainer() {
    val PLUGIN_ORDER: KtSourcelessDiagnosticFactory by SourcelessDiagnosticFactoryDelegateProvider(
        severity = Severity.ERROR,
        container = this,
    )

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = BridgeDiagnosticRenderers
}

internal object BridgeDiagnosticRenderers : BaseSourcelessDiagnosticRendererFactory() {
    override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("BridgeDiagnostics") { map ->
        map.put(factory = BridgeDiagnostics.PLUGIN_ORDER, message = MESSAGE_PLACEHOLDER)
    }
}
