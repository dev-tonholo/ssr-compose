package dev.tonholo.ssrcompose.compiler.consumer.renderer.fir.checker

import dev.tonholo.ssrcompose.compiler.SsrNames
import dev.tonholo.ssrcompose.compiler.consumer.MarkableFunction
import dev.tonholo.ssrcompose.compiler.consumer.renderer.diagnostic.RendererDiagnostics
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirDeclarationChecker
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.name.CallableId

/**
 * Reports a compile error at the declaration of each overloaded `@ConsumerComposable` in the same package. The shape
 * of a marked function is checked by the consumer checker.
 */
internal class RendererComposableChecker(
    private val session: FirSession,
) : FirDeclarationChecker<FirNamedFunction>(MppCheckerKind.Platform) {
    private val markable = MarkableFunction(session = session)

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirNamedFunction) {
        if (!markable.isMarked(function = declaration) || !markable.isTopLevel(function = declaration)) return
        val callableId = declaration.symbol.callableId
        if (hasDuplicateMarkedComposable(callableId = callableId)) {
            reporter.reportOn(
                source = declaration.source,
                factory = RendererDiagnostics.INVALID_MARKED_COMPOSABLE,
                a = duplicateComposableMessage(
                    composable = declaration.name.asString(),
                    packageName = callableId.packageName.asString(),
                ),
            )
        }
    }

    private fun hasDuplicateMarkedComposable(callableId: CallableId): Boolean =
        session.firProvider.symbolProvider
            .getTopLevelFunctionSymbols(packageFqName = callableId.packageName, name = callableId.callableName)
            .count { candidate ->
                !candidate.rawStatus.isExpect &&
                    candidate.hasAnnotation(classId = SsrNames.consumerComposable, session = session)
            } > 1

    private fun duplicateComposableMessage(composable: String, packageName: String): String =
        "The marked composable '$composable' is declared more than once in package '$packageName'. " +
            "Marked composables in the same package cannot be overloaded."
}
