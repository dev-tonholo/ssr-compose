@file:OptIn(ExperimentalTopLevelDeclarationsGenerationApi::class)

package dev.tonholo.ssrcompose.compiler.consumer.renderer.fir

import dev.tonholo.ssrcompose.compiler.SsrNames
import dev.tonholo.ssrcompose.compiler.consumer.renderer.RendererKey
import dev.tonholo.ssrcompose.compiler.consumer.renderer.RendererNames
import dev.tonholo.ssrcompose.compiler.consumer.renderer.RendererParameter
import dev.tonholo.ssrcompose.compiler.fir.buildComposableAnnotation
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.ExperimentalTopLevelDeclarationsGenerationApi
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.LookupPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.plugin.createTopLevelFunction
import org.jetbrains.kotlin.fir.resolve.providers.getRegularClassSymbolByClassId
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.fir.types.isString
import org.jetbrains.kotlin.name.CallableId

/**
 * Declares one top-level `@Composable` renderer per valid marked composable whose regular parameters are all `String`,
 * when `PlacedComponentScope` is available in the compilation. Each renderer gets its own synthetic file name so
 * recompiling one source file does not overwrite another file's renderer class and a user file cannot clash with it.
 */
internal class RendererDeclarationGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    private val predicate: LookupPredicate = LookupPredicate.create {
        annotated(SsrNames.consumerComposable.asSingleFqName())
    }
    private val composablesByRendererId: Map<CallableId, FirNamedFunctionSymbol> by lazy {
        buildComposablesByRendererId()
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(predicate)
    }

    override fun getTopLevelCallableIds(): Set<CallableId> = composablesByRendererId.keys

    override fun generateFunctions(
        callableId: CallableId,
        context: MemberGenerationContext?,
    ): List<FirNamedFunctionSymbol> {
        val composable = composablesByRendererId[callableId] ?: return emptyList()
        val function = createTopLevelFunction(
            key = rendererKey(composable = composable),
            callableId = callableId,
            returnType = session.builtinTypes.unitType.coneType,
            containingFileName = RendererNames.containingFileName(composable = composable.callableId),
        ) {
            valueParameter(
                name = RendererNames.placedParameter,
                type = RendererNames.placedComponentScope.constructClassLikeType(),
            )
        }
        function.replaceAnnotations(newAnnotations = listOf(buildComposableAnnotation()))
        return listOf(function.symbol)
    }

    private fun buildComposablesByRendererId(): Map<CallableId, FirNamedFunctionSymbol> {
        if (!hasPlacedComponentScope()) return emptyMap()
        return uniqueMarkedComposables()
            .filter(::canGenerateRenderer)
            .associateBy { symbol -> RendererNames.rendererCallableId(composable = symbol.callableId) }
    }

    private fun uniqueMarkedComposables(): List<FirNamedFunctionSymbol> =
        markedComposables()
            .groupBy { symbol -> symbol.callableId }
            .values
            .mapNotNull { symbols -> symbols.singleOrNull() }

    private fun canGenerateRenderer(composable: FirNamedFunctionSymbol): Boolean =
        composable.receiverParameterSymbol == null &&
            composable.contextParameterSymbols.isEmpty() &&
            composable.typeParameterSymbols.isEmpty() &&
            composable.rawStatus.visibility != Visibilities.Private &&
            composable.valueParameterSymbols.all { parameter -> parameter.resolvedReturnType.isString }

    private fun rendererKey(composable: FirNamedFunctionSymbol): RendererKey =
        RendererKey(
            composable = composable.callableId,
            parameters = composable.valueParameterSymbols.map { parameter ->
                RendererParameter(
                    name = parameter.name,
                    type = checkNotNull(parameter.resolvedReturnType.classId) {
                        "The parameter ${parameter.name} of ${composable.callableId} has no ClassId"
                    },
                )
            },
        )

    private fun hasPlacedComponentScope(): Boolean =
        session.getRegularClassSymbolByClassId(classId = RendererNames.placedComponentScope) != null

    private fun markedComposables(): List<FirNamedFunctionSymbol> =
        session.predicateBasedProvider
            .getSymbolsByPredicate(predicate = predicate)
            .filterIsInstance<FirNamedFunctionSymbol>()
            .filter { symbol -> symbol.callableId.classId == null && !symbol.rawStatus.isExpect }
}
