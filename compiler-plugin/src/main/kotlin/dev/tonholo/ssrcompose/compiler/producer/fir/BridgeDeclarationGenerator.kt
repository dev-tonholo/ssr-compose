@file:OptIn(ExperimentalTopLevelDeclarationsGenerationApi::class)

package dev.tonholo.ssrcompose.compiler.producer.fir

import dev.tonholo.ssrcompose.compiler.producer.BridgeCatalog
import dev.tonholo.ssrcompose.compiler.producer.ProducerNames
import dev.tonholo.ssrcompose.compiler.producer.throwaway.ThrowawayComponent
import dev.tonholo.ssrcompose.compiler.producer.throwaway.ThrowawayParameter
import dev.tonholo.ssrcompose.compiler.producer.throwaway.ThrowawayParameterKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.impl.FirEmptyAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.extensions.ExperimentalTopLevelDeclarationsGenerationApi
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.plugin.createTopLevelFunction
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeAttributes
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.CustomAnnotationTypeAttribute
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

internal class BridgeDeclarationGenerator(
    session: FirSession,
    private val catalog: BridgeCatalog,
) : FirDeclarationGenerationExtension(session) {
    private val componentsById: Map<CallableId, ThrowawayComponent> =
        catalog.index.components.associateBy { CallableId(catalog.bridgePackage, Name.identifier(it.name)) }

    override fun hasPackage(packageFqName: FqName): Boolean = packageFqName == catalog.bridgePackage

    override fun getTopLevelCallableIds(): Set<CallableId> = componentsById.keys

    override fun generateFunctions(
        callableId: CallableId,
        context: MemberGenerationContext?,
    ): List<FirNamedFunctionSymbol> {
        val component = componentsById[callableId] ?: return emptyList()
        val function = createTopLevelFunction(
            key = catalog.key,
            callableId = callableId,
            returnType = session.builtinTypes.unitType.coneType,
        ) {
            component.parameters.forEach { parameter ->
                valueParameter(
                    name = Name.identifier(parameter.name),
                    type = parameter.coneType(),
                    hasDefaultValue = !parameter.isRequired,
                )
            }
        }
        function.replaceAnnotations(listOf(composableAnnotation()))
        return listOf(function.symbol)
    }

    private fun ThrowawayParameter.coneType(): ConeKotlinType =
        when (kind) {
            ThrowawayParameterKind.Text ->
                StandardClassIds.String.constructClassLikeType(isMarkedNullable = !isRequired)

            ThrowawayParameterKind.ActionOfText -> ProducerNames.remoteAction1.constructClassLikeType(
                typeArguments = arrayOf(session.builtinTypes.stringType.coneType),
            )

            ThrowawayParameterKind.Slot -> ProducerNames.composableFunction0.constructClassLikeType(
                typeArguments = arrayOf(session.builtinTypes.unitType.coneType),
                isMarkedNullable = !isRequired,
                attributes = composableTypeAttributes(),
            )

            ThrowawayParameterKind.SlotOfText -> ProducerNames.composableFunction1.constructClassLikeType(
                typeArguments = arrayOf(
                    session.builtinTypes.stringType.coneType,
                    session.builtinTypes.unitType.coneType,
                ),
                attributes = composableTypeAttributes(),
            )
        }

    private fun composableTypeAttributes(): ConeAttributes =
        ConeAttributes.create(listOf(CustomAnnotationTypeAttribute(listOf(composableAnnotation()))))

    private fun composableAnnotation() = buildAnnotation {
        annotationTypeRef = buildResolvedTypeRef { coneType = ProducerNames.composable.constructClassLikeType() }
        argumentMapping = FirEmptyAnnotationArgumentMapping
    }
}
