package dev.tonholo.ssrcompose.compiler.testing

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirExpressionChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.getAnnotationWithResolvedArgumentsByClassId
import org.jetbrains.kotlin.fir.expressions.FirCollectionLiteral
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.FirGetClassCall
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Records, for each call to a plugin-created function, the marker classes of the `@OptIn` on that function's FIR
 * declaration. `kotlin.OptIn` has source retention, so only the compiler's own view of the declaration shows it.
 */
internal class CalledBridgeOptIns : CompilerPluginRegistrar() {
    private val recorded = mutableListOf<List<ClassId>>()

    /** One entry per call to a plugin-created function: the marker classes it opts in to, empty when it has none. */
    val markersPerCall: List<List<ClassId>> get() = recorded.toList()

    override val pluginId: String = "dev.tonholo.ssrcompose.test.called-bridge-opt-ins"

    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        FirExtensionRegistrarAdapter.registerExtension(
            object : FirExtensionRegistrar() {
                override fun ExtensionRegistrarContext.configurePlugin() {
                    +FirAdditionalCheckersExtension.Factory { session -> Checks(session = session) }
                }
            },
        )
    }

    private inner class Checks(session: FirSession) : FirAdditionalCheckersExtension(session) {
        override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
            override val functionCallCheckers = setOf(CallRecorder())
        }
    }

    private inner class CallRecorder : FirExpressionChecker<FirFunctionCall>(MppCheckerKind.Common) {
        context(context: CheckerContext, reporter: DiagnosticReporter)
        override fun check(expression: FirFunctionCall) {
            val function = expression.calleeReference.toResolvedCallableSymbol() ?: return
            if (function.origin !is FirDeclarationOrigin.Plugin) return
            val optIn = function.getAnnotationWithResolvedArgumentsByClassId(OPT_IN, context.session)
            val markers = optIn?.argumentMapping?.mapping?.get(MARKER_CLASS) as? FirCollectionLiteral
            recorded += markers?.argumentList?.arguments.orEmpty().mapNotNull { argument ->
                (argument as? FirGetClassCall)?.argument?.resolvedType?.classId
            }
        }
    }

    private companion object {
        val OPT_IN = ClassId(FqName("kotlin"), Name.identifier("OptIn"))
        val MARKER_CLASS: Name = Name.identifier("markerClass")
    }
}
