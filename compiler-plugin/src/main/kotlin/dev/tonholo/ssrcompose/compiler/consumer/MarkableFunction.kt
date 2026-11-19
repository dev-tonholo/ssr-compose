package dev.tonholo.ssrcompose.compiler.consumer

import dev.tonholo.ssrcompose.compiler.SsrNames
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.name.ClassId

/**
 * The one rule for which functions can carry `@ConsumerComposable`: a top-level, public or internal, non-generic
 * `@Composable` function without an extension receiver or context parameters.
 */
internal class MarkableFunction(private val session: FirSession) {
    /**
     * True for a function that carries `@ConsumerComposable`. A marked `expect` is left out, because its marked
     * `actual` stands for the pair.
     */
    fun isMarked(function: FirNamedFunction): Boolean =
        !function.status.isExpect && function.isAnnotated(classId = SsrNames.consumerComposable)

    /**
     * True for a top-level function, which is neither a member nor a local function.
     */
    fun isTopLevel(function: FirNamedFunction): Boolean =
        !function.isLocal && function.symbol.callableId.classId == null

    /**
     * What stops [function] from being marked, or an empty list when it is a markable function or has nothing to
     * do with marking.
     */
    fun problemsOf(function: FirNamedFunction): List<FunctionProblem> =
        when {
            isMarked(function = function) -> shapeProblemsOf(function = function)
            else -> emptyList()
        }

    fun concerns(function: FirNamedFunction): Boolean = isMarked(function = function)

    private fun shapeProblemsOf(function: FirNamedFunction): List<FunctionProblem> =
        listOfNotNull(
            FunctionProblem.NOT_COMPOSABLE.takeUnless { function.isAnnotated(classId = SsrNames.composable) },
            FunctionProblem.NOT_TOP_LEVEL.takeUnless { isTopLevel(function = function) },
            FunctionProblem.EXTENSION_RECEIVER.takeIf { function.receiverParameter != null },
            FunctionProblem.CONTEXT_PARAMETERS.takeIf { function.contextParameters.isNotEmpty() },
            FunctionProblem.TYPE_PARAMETERS.takeIf { function.typeParameters.isNotEmpty() },
            FunctionProblem.PRIVATE.takeIf { function.status.visibility == Visibilities.Private },
        )

    private fun FirNamedFunction.isAnnotated(classId: ClassId): Boolean =
        hasAnnotation(classId = classId, session = session)
}
