package dev.tonholo.ssrcompose.compiler.consumer

import dev.tonholo.ssrcompose.compiler.consumer.renderer.diagnostic.RendererDiagnostics
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirDeclarationChecker
import org.jetbrains.kotlin.fir.declarations.FirFile

/**
 * Writes the fragment of every source file the compiler checks, when there is a fragments folder. Fails the
 * compilation for a function that cannot be marked, a marked composable with a parameter the catalog cannot describe,
 * or a file whose name cannot name a fragment. A file with any such error writes no fragment.
 */
internal class ConsumerComposableChecker(
    private val writer: FragmentWriter?,
    private val messages: MessageCollector,
) : FirDeclarationChecker<FirFile>(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFile) {
        val scanned = MarkedComposables(session = context.session).scan(file = declaration)
        scanned.errors.forEach { error ->
            val sourceElement = error.sourceElement ?: declaration.source
            if (sourceElement != null) {
                reporter.reportOn(
                    source = sourceElement,
                    factory = RendererDiagnostics.INVALID_MARKED_COMPOSABLE,
                    a = error.message,
                )
            } else {
                messages.report(severity = CompilerMessageSeverity.ERROR, message = error.message)
            }
        }
        scanned.fragmentOrNull()?.let { fragment -> writer?.save(fragment = fragment) }
    }
}
