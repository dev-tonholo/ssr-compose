package dev.tonholo.ssrcompose.compiler.consumer

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirDeclarationChecker
import org.jetbrains.kotlin.fir.declarations.FirFile

/**
 * Writes the fragment of every source file the compiler checks, and fails the compilation for a marked composable
 * with a parameter that is not text or a composable slot, or in a file whose name cannot name a fragment.
 */
internal class ConsumerComposableChecker(
    private val writer: FragmentWriter,
    private val messages: MessageCollector,
) : FirDeclarationChecker<FirFile>(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFile) {
        val scanned = MarkedComposables(session = context.session).scan(file = declaration)
        scanned.errors.forEach { error ->
            messages.report(severity = CompilerMessageSeverity.ERROR, message = error.message)
        }
        scanned.fragmentOrNull()?.let { fragment -> writer.save(fragment = fragment) }
    }
}
