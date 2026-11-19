@file:OptIn(SymbolInternals::class)

package dev.tonholo.ssrcompose.compiler.producer.fir

import dev.tonholo.ssrcompose.compiler.producer.diagnostic.BridgeDiagnostics
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFileChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirExpressionChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.expressions.FirCallableReferenceAccess
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirQualifiedAccessExpression
import org.jetbrains.kotlin.fir.expressions.FirResolvedQualifier
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedError
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * Explains why a named component has no bridge. Nothing is declared for such a component, so code that only mentions
 * its name still compiles, and only an import, call or reference that Kotlin cannot resolve gets the error.
 */
internal class RefusedBridgeCheckers(
    session: FirSession,
    private val refusals: Map<CallableId, String>,
) : FirAdditionalCheckersExtension(session) {
    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val fileCheckers: Set<FirFileChecker> = setOf(ImportChecker())
    }

    override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
        override val functionCallCheckers = setOf(CallChecker())
        override val callableReferenceAccessCheckers = setOf(ReferenceChecker())
    }

    private inner class ImportChecker : FirFileChecker(MppCheckerKind.Common) {
        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun check(declaration: FirFile) {
            declaration.imports.filterNot { it.isAllUnder }.forEach { import ->
                val imported = import.importedFqName ?: return@forEach
                val id = CallableId(imported.parent(), imported.shortName())
                val refusal = refusals[id]
                if (refusal != null && !isDeclared(id = id)) {
                    reporter.reportOn(
                        source = import.source,
                        factory = BridgeDiagnostics.UNPLACEABLE_COMPONENT,
                        a = refusal,
                    )
                }
            }
        }
    }

    private inner class CallChecker : FirExpressionChecker<FirFunctionCall>(MppCheckerKind.Common) {
        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun check(expression: FirFunctionCall) = reportIfRefused(expression)
    }

    private inner class ReferenceChecker : FirExpressionChecker<FirCallableReferenceAccess>(MppCheckerKind.Common) {
        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun check(expression: FirCallableReferenceAccess) = reportIfRefused(expression)
    }

    private fun isDeclared(id: CallableId): Boolean {
        val provider = session.symbolProvider
        return provider.getTopLevelCallableSymbols(id.packageName, id.callableName).isNotEmpty() ||
            provider.getClassLikeSymbolByClassId(ClassId(id.packageName, id.callableName)) != null
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun reportIfRefused(expression: FirQualifiedAccessExpression) {
        val reference = (expression.calleeReference as? FirErrorNamedReference)
            ?.takeIf { it.diagnostic is ConeUnresolvedError }
            ?: return
        val refusal = packagesSeenBy(expression = expression).firstNotNullOfOrNull { packageName ->
            refusals[CallableId(packageName, reference.name)]
        } ?: return
        reporter.reportOn(source = expression.source, factory = BridgeDiagnostics.UNPLACEABLE_COMPONENT, a = refusal)
    }

    context(context: CheckerContext)
    private fun packagesSeenBy(expression: FirQualifiedAccessExpression): List<FqName> =
        when (val receiver = expression.explicitReceiver) {
            null -> packagesOpenInFile()
            is FirResolvedQualifier ->
                listOfNotNull(receiver.packageFqName.takeIf { receiver.relativeClassFqName == null })

            else -> emptyList()
        }

    context(context: CheckerContext)
    private fun packagesOpenInFile(): List<FqName> {
        val file = context.containingFileSymbol?.fir ?: return emptyList()
        val starImports = file.imports.filter { it.isAllUnder }.mapNotNull { it.importedFqName }
        return starImports + file.packageDirective.packageFqName
    }
}
