@file:OptIn(ExperimentalTopLevelDeclarationsGenerationApi::class)

package dev.tonholo.ssrcompose.compiler.producer.fir

import dev.tonholo.ssrcompose.compiler.fir.buildComposableAnnotation
import dev.tonholo.ssrcompose.compiler.producer.BridgeCatalog
import dev.tonholo.ssrcompose.compiler.producer.BridgeComponent
import dev.tonholo.ssrcompose.compiler.producer.BridgeParameter
import dev.tonholo.ssrcompose.compiler.producer.BridgeParameterKind
import dev.tonholo.ssrcompose.compiler.producer.ProducerNames
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.utils.fileNameForPluginGeneratedCallable
import org.jetbrains.kotlin.fir.extensions.ExperimentalTopLevelDeclarationsGenerationApi
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.plugin.createTopLevelFunction
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeAttributes
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.CustomAnnotationTypeAttribute
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

internal class BridgeDeclarationGenerator(
    session: FirSession,
    private val catalog: BridgeCatalog,
    private val unplaceableBridgeMarker: UnplaceableBridgeMarker = UnplaceableBridgeMarker(),
) : FirDeclarationGenerationExtension(session) {
    private val internalApiOptIn = InternalApiOptIn(session = session)
    private val componentsById: Map<CallableId, BridgeComponent> =
        catalog.components.associateBy { CallableId(catalog.bridgePackage, Name.identifier(it.name)) }

    override fun hasPackage(packageFqName: FqName): Boolean = catalog.bridgePackage.startsWith(packageFqName)

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
            visibility = Visibilities.Internal
            component.parameters.forEach { parameter ->
                valueParameter(
                    name = Name.identifier(parameter.name),
                    type = parameter.coneType(),
                    hasDefaultValue = !parameter.isRequired,
                )
            }
        }
        function.fileNameForPluginGeneratedCallable = catalog.classRule.fileNameOf(componentName = component.name)
        function.replaceAnnotations(listOfNotNull(buildComposableAnnotation(), internalApiOptIn.annotationOrNull()))
        unplaceableBridgeMarker.mark(function = function, component = component)
        return listOf(function.symbol)
    }

    private fun BridgeParameter.coneType(): ConeKotlinType =
        when (kind) {
            BridgeParameterKind.Text ->
                StandardClassIds.String.constructClassLikeType(isMarkedNullable = !isRequired)

            BridgeParameterKind.ActionOfText -> ProducerNames.remoteAction1.constructClassLikeType(
                typeArguments = arrayOf(session.builtinTypes.stringType.coneType),
            )

            BridgeParameterKind.Slot -> ProducerNames.composableFunction0.constructClassLikeType(
                typeArguments = arrayOf(session.builtinTypes.unitType.coneType),
                isMarkedNullable = !isRequired,
                attributes = composableTypeAttributes(),
            )

            BridgeParameterKind.SlotOfText -> ProducerNames.composableFunction1.constructClassLikeType(
                typeArguments = arrayOf(
                    session.builtinTypes.stringType.coneType,
                    session.builtinTypes.unitType.coneType,
                ),
                attributes = composableTypeAttributes(),
            )
        }

    private fun composableTypeAttributes(): ConeAttributes =
        ConeAttributes.create(listOf(CustomAnnotationTypeAttribute(listOf(buildComposableAnnotation()))))
}
