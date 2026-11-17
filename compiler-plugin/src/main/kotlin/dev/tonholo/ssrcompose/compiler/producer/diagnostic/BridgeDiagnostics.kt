package dev.tonholo.ssrcompose.compiler.producer.diagnostic

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.BaseSourcelessDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers

internal object BridgeDiagnostics : KtDiagnosticsContainer() {
    val UNPLACEABLE_COMPONENT: KtDiagnosticFactory1<String> by error1<PsiElement, String>()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = BridgeDiagnosticRenderers
}

internal object BridgeDiagnosticRenderers : BaseSourcelessDiagnosticRendererFactory() {
    override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("BridgeDiagnostics") { map ->
        map.put(
            factory = BridgeDiagnostics.UNPLACEABLE_COMPONENT,
            message = MESSAGE_PLACEHOLDER,
            rendererA = CommonRenderers.STRING,
        )
    }
}
