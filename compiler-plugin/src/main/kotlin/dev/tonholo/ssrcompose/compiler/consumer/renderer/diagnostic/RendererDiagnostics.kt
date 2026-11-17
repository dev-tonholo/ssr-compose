package dev.tonholo.ssrcompose.compiler.consumer.renderer.diagnostic

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.BaseSourcelessDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers

/**
 * Compiler diagnostics reported on invalid `@ConsumerComposable` declarations during FIR analysis.
 */
internal object RendererDiagnostics : KtDiagnosticsContainer() {
    val INVALID_MARKED_COMPOSABLE: KtDiagnosticFactory1<String> by error1<PsiElement, String>()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = RendererDiagnosticRenderers
}

internal object RendererDiagnosticRenderers : BaseSourcelessDiagnosticRendererFactory() {
    override val MAP: KtDiagnosticFactoryToRendererMap by
        KtDiagnosticFactoryToRendererMap("RendererDiagnostics") { map ->
            map.put(
                factory = RendererDiagnostics.INVALID_MARKED_COMPOSABLE,
                message = MESSAGE_PLACEHOLDER,
                rendererA = CommonRenderers.STRING,
            )
        }
}
