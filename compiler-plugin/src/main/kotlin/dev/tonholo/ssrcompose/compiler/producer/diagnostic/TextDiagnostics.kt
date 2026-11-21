package dev.tonholo.ssrcompose.compiler.producer.diagnostic

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactoryToRendererMap
import org.jetbrains.kotlin.diagnostics.KtDiagnosticsContainer
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.rendering.BaseDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.BaseSourcelessDiagnosticRendererFactory
import org.jetbrains.kotlin.diagnostics.rendering.CommonRenderers

internal object TextDiagnostics : KtDiagnosticsContainer() {
    val TEXT_OVERLOAD_NOT_SUPPORTED: KtDiagnosticFactory1<String> by error1<PsiElement, String>()

    override fun getRendererFactory(): BaseDiagnosticRendererFactory = TextDiagnosticRenderers
}

internal object TextDiagnosticRenderers : BaseSourcelessDiagnosticRendererFactory() {
    override val MAP: KtDiagnosticFactoryToRendererMap by KtDiagnosticFactoryToRendererMap("TextDiagnostics") { map ->
        map.put(
            factory = TextDiagnostics.TEXT_OVERLOAD_NOT_SUPPORTED,
            message = MESSAGE_PLACEHOLDER,
            rendererA = CommonRenderers.STRING,
        )
    }
}
