package dev.tonholo.ssrcompose.compiler.consumer.renderer.fir.checker

import dev.tonholo.ssrcompose.compiler.SsrNames
import dev.tonholo.ssrcompose.compiler.consumer.renderer.diagnostic.RendererDiagnostics
import org.jetbrains.kotlin.descriptors.Visibilities
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
 * Reports a compile error at the declaration of each overloaded `@ConsumerComposable` in the same package and each
 * `@ConsumerComposable` that declares an extension receiver, context parameters or type parameters, or is private.
 */
internal class RendererComposableChecker(
    private val session: FirSession,
) : FirDeclarationChecker<FirNamedFunction>(MppCheckerKind.Platform) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirNamedFunction) {
        if (!declaration.isTopLevelMarkedComposable()) return
        val name = declaration.name.asString()
        if (declaration.receiverParameter != null) {
            reporter.reportOn(
                source = declaration.source,
                factory = RendererDiagnostics.INVALID_MARKED_COMPOSABLE,
                a = extensionReceiverMessage(composable = name),
            )
        }
        if (declaration.contextParameters.isNotEmpty()) {
            reporter.reportOn(
                source = declaration.source,
                factory = RendererDiagnostics.INVALID_MARKED_COMPOSABLE,
                a = contextParametersMessage(composable = name),
            )
        }
        if (declaration.typeParameters.isNotEmpty()) {
            reporter.reportOn(
                source = declaration.source,
                factory = RendererDiagnostics.INVALID_MARKED_COMPOSABLE,
                a = typeParametersMessage(composable = name),
            )
        }
        if (declaration.status.visibility == Visibilities.Private) {
            reporter.reportOn(
                source = declaration.source,
                factory = RendererDiagnostics.INVALID_MARKED_COMPOSABLE,
                a = privateMessage(composable = name),
            )
        }
        val callableId = declaration.symbol.callableId
        if (hasDuplicateMarkedComposable(callableId = callableId)) {
            reporter.reportOn(
                source = declaration.source,
                factory = RendererDiagnostics.INVALID_MARKED_COMPOSABLE,
                a = duplicateComposableMessage(
                    composable = name,
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

    private fun FirNamedFunction.isTopLevelMarkedComposable(): Boolean =
        !isLocal &&
            !status.isExpect &&
            symbol.callableId.classId == null &&
            hasAnnotation(classId = SsrNames.consumerComposable, session = session)

    private fun extensionReceiverMessage(composable: String): String =
        "The marked composable '$composable' has an extension receiver. " +
            "Only top-level composables without an extension receiver or context parameters can be marked."

    private fun contextParametersMessage(composable: String): String =
        "The marked composable '$composable' has context parameters. " +
            "Only top-level composables without an extension receiver or context parameters can be marked."

    private fun typeParametersMessage(composable: String): String =
        "The marked composable '$composable' has type parameters. Only non-generic composables can be marked."

    private fun privateMessage(composable: String): String =
        "The marked composable '$composable' is private. Only internal or public composables can be marked."

    private fun duplicateComposableMessage(composable: String, packageName: String): String =
        "The marked composable '$composable' is declared more than once in package '$packageName'. " +
            "Marked composables in the same package cannot be overloaded."
}
