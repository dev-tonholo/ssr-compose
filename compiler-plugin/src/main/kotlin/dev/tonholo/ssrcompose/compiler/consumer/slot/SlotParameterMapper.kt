package dev.tonholo.ssrcompose.compiler.consumer.slot

import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.catalog.model.SlotAccepts
import dev.tonholo.ssrcompose.compiler.SsrNames
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.abbreviatedType
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.customAnnotations
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.fir.types.isSomeFunctionType
import org.jetbrains.kotlin.fir.types.isUnit
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.name.ClassId

/**
 * Maps a composable slot parameter (`@Composable () -> Unit`) to [Parameter.Slot], reading
 * `@ConsumerComposable` and `@ProducerComposable` from the slot's type.
 */
internal object SlotParameterMapper {
    fun isSlot(parameter: FirValueParameter, session: FirSession): Boolean =
        parameter.returnTypeRef.coneType.let { type ->
            !type.isMarkedNullable &&
                type.isSomeFunctionType(session = session) &&
                type.isUnitFunction0() &&
                SsrNames.composable in type.annotationClassIds(session = session)
        }

    fun hasConflictingAnnotations(parameter: FirValueParameter, session: FirSession): Boolean {
        if (!isSlot(parameter = parameter, session = session)) return false
        val annotations = parameter.returnTypeRef.coneType.annotationClassIds(session = session)
        return SsrNames.consumerComposable in annotations && SsrNames.producerComposable in annotations
    }

    fun toSlot(parameter: FirValueParameter, session: FirSession): Parameter.Slot {
        val annotations = parameter.returnTypeRef.coneType.annotationClassIds(session = session)
        val accepts = when {
            SsrNames.consumerComposable in annotations -> SlotAccepts.Consumer
            SsrNames.producerComposable in annotations -> SlotAccepts.Producer
            else -> SlotAccepts.Any
        }
        return Parameter.Slot(
            name = parameter.name.asString(),
            accepts = accepts,
            required = parameter.defaultValue == null,
        )
    }

    private fun ConeKotlinType.isUnitFunction0(): Boolean {
        val classLike = this as? ConeClassLikeType ?: return false
        val arguments = classLike.typeArguments
        return arguments.size == 1 && arguments.first().type?.isUnit == true
    }

    private fun ConeKotlinType.annotationClassIds(session: FirSession): Set<ClassId> {
        val collected = mutableSetOf<ClassId>()
        val visited = mutableSetOf<FirTypeAliasSymbol>()
        var current: ConeKotlinType? = this
        while (current != null) {
            current.customAnnotations.mapNotNullTo(collected) { it.toAnnotationClassId(session = session) }
            val abbreviated = current.attributes.abbreviatedType?.coneType as? ConeClassLikeType
            val aliasSymbol = abbreviated?.lookupTag?.toSymbol(useSiteSession = session) as? FirTypeAliasSymbol
            if (aliasSymbol == null || !visited.add(aliasSymbol)) {
                current = null
            } else {
                aliasSymbol.resolvedAnnotationsWithClassIds.mapNotNullTo(collected) {
                    it.toAnnotationClassId(session = session)
                }
                current = aliasSymbol.resolvedExpandedTypeRef.coneType
            }
        }
        return collected
    }
}
