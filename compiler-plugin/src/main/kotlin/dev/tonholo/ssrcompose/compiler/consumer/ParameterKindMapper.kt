package dev.tonholo.ssrcompose.compiler.consumer

import dev.tonholo.ssrcompose.catalog.model.ChoiceList
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.catalog.validation.isKotlinIdentifier
import dev.tonholo.ssrcompose.compiler.SsrNames
import dev.tonholo.ssrcompose.compiler.consumer.slot.SlotParameterMapper
import dev.tonholo.ssrcompose.core.data.ValueKind
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.collectEnumEntries
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isMarkedNullable
import org.jetbrains.kotlin.fir.types.type
import org.jetbrains.kotlin.name.StandardClassIds

/**
 * What mapping a parameter of a marked composable produced.
 */
internal sealed interface ParameterMappingResult {
    data class Supported(val parameter: Parameter, val choice: ChoiceList? = null) : ParameterMappingResult
    data object Skipped : ParameterMappingResult
    data object Unsupported : ParameterMappingResult
}

/**
 * Turns the parameter of a marked composable into a catalog parameter.
 */
internal object ParameterKindMapper {
    fun map(parameter: FirValueParameter, session: FirSession): ParameterMappingResult {
        val expandedType = parameter.returnTypeRef.coneType
        return when {
            expandedType.classId == SsrNames.modifier -> ParameterMappingResult.Skipped
            SlotParameterMapper.isSlot(parameter = parameter, session = session) ->
                ParameterMappingResult.Supported(
                    parameter = SlotParameterMapper.toSlot(parameter = parameter, session = session),
                )
            else -> mapValueParameter(parameter = parameter, expandedType = expandedType, session = session)
        }
    }

    private fun mapValueParameter(
        parameter: FirValueParameter,
        expandedType: ConeKotlinType,
        session: FirSession,
    ): ParameterMappingResult {
        val valueKind = valueKindOf(expandedType = expandedType, session = session)
            ?: return ParameterMappingResult.Unsupported
        return ParameterMappingResult.Supported(
            parameter = Parameter.Value(
                name = parameter.name.asString(),
                kind = valueKind.kind,
                required = parameter.defaultValue == null,
                nullable = expandedType.isMarkedNullable,
            ),
            choice = valueKind.choice,
        )
    }

    private fun valueKindOf(expandedType: ConeKotlinType, session: FirSession): MappedValueKind? =
        if (expandedType.classId == StandardClassIds.List) {
            listValueKindOf(listType = expandedType, session = session)
        } else {
            elementValueKindOf(expandedType = expandedType, session = session)
        }

    private fun listValueKindOf(listType: ConeKotlinType, session: FirSession): MappedValueKind? =
        listType.typeArguments.singleOrNull()?.type
            ?.fullyExpandedType(useSiteSession = session)
            ?.takeUnless { it.isMarkedNullable }
            ?.let { itemType -> elementValueKindOf(expandedType = itemType, session = session) }
            ?.let { element ->
                MappedValueKind(kind = ValueKind.ListOf(element = element.kind), choice = element.choice)
            }

    private fun elementValueKindOf(expandedType: ConeKotlinType, session: FirSession): MappedValueKind? =
        when (expandedType.classId) {
            StandardClassIds.String -> MappedValueKind(kind = ValueKind.Text)
            StandardClassIds.Int, StandardClassIds.Long -> MappedValueKind(kind = ValueKind.Whole)
            StandardClassIds.Float, StandardClassIds.Double -> MappedValueKind(kind = ValueKind.Decimal)
            StandardClassIds.Boolean -> MappedValueKind(kind = ValueKind.Flag)
            else -> choiceValueKindOf(expandedType = expandedType, session = session)
        }

    private fun choiceValueKindOf(expandedType: ConeKotlinType, session: FirSession): MappedValueKind? =
        expandedType.toRegularClassSymbol(session = session)
            ?.takeIf { it.classKind == ClassKind.ENUM_CLASS }
            ?.let { symbol -> choiceListOf(symbol = symbol, session = session) }
            ?.let { choice -> MappedValueKind(kind = ValueKind.Choice(listName = choice.name), choice = choice) }

    private fun choiceListOf(symbol: FirRegularClassSymbol, session: FirSession): ChoiceList? {
        val name = symbol.name.asString()
        val entries = symbol.collectEnumEntries(session = session).map { entry -> entry.name.asString() }
        val isValid = isKotlinIdentifier(text = name) &&
            entries.isNotEmpty() &&
            entries.all { entry -> isKotlinIdentifier(text = entry) }
        return if (isValid) ChoiceList(name = name, entries = entries) else null
    }

    private data class MappedValueKind(val kind: ValueKind, val choice: ChoiceList? = null)
}
