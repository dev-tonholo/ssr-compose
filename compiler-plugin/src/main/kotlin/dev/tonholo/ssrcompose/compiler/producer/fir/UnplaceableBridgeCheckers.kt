package dev.tonholo.ssrcompose.compiler.producer.fir

import dev.tonholo.ssrcompose.compiler.producer.ProducerNames
import dev.tonholo.ssrcompose.compiler.producer.diagnostic.BridgeDiagnostics
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirExpressionChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.getAnnotationWithResolvedArgumentsByClassId
import org.jetbrains.kotlin.fir.declarations.getStringArgument
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol

/**
 * Reports the error carried by the `UnplaceableBridge` annotation on every call and every reference to a function that
 * has it, whether the function is created in this module or read from a compiled one.
 */
internal class UnplaceableBridgeCheckers(session: FirSession) : FirAdditionalCheckersExtension(session) {
    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
        override val functionCallCheckers = setOf(CallChecker())
        override val callableReferenceAccessCheckers = setOf(ReferenceChecker())
    }

    private inner class CallChecker : FirExpressionChecker<FirFunctionCall>(MppCheckerKind.Common) {
        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun check(expression: FirFunctionCall) = reportIfUnplaceable(expression)
    }

    private inner class ReferenceChecker : FirExpressionChecker<FirCallableReferenceAccess>(MppCheckerKind.Common) {
        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun check(expression: FirCallableReferenceAccess) = reportIfUnplaceable(expression)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportIfUnplaceable(expression: FirQualifiedAccessExpression) {
        val function = expression.calleeReference.toResolvedCallableSymbol() ?: return
        val marker = function.getAnnotationWithResolvedArgumentsByClassId(ProducerNames.unplaceableBridge, session)
        val message = marker?.getStringArgument(ProducerNames.message) ?: return
        reporter.reportOn(
            source = expression.source,
            factory = BridgeDiagnostics.UNPLACEABLE_COMPONENT,
            a = message,
        )
    }
}
