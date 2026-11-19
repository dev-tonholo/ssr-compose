package dev.tonholo.ssrcompose.compiler.consumer

import dev.tonholo.ssrcompose.catalog.model.ChoiceList
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.catalog.validation.CatalogLimits
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
    data class Unsupported(val reason: String) : ParameterMappingResult
}

/**
 * Turns the parameter of a marked composable into a catalog parameter.
 */
internal object ParameterKindMapper {
    private const val UNSUPPORTED_TYPE_REASON =
        "its type is not a supported value, choice, list or composable slot"
    private const val NULLABLE_LIST_ELEMENT_REASON = "list elements must not be nullable"
    private const val NESTED_LIST_REASON =
        "a list holds text, whole numbers, decimal numbers, flags or choices, never another list"
    private const val UNSUPPORTED_LIST_ELEMENT_REASON =
        "a list holds text, whole numbers, decimal numbers, flags or choices"

    fun map(
        parameter: FirValueParameter,
        session: FirSession,
        hasDefault: Boolean = parameter.defaultValue != null,
    ): ParameterMappingResult {
        val expandedType = parameter.returnTypeRef.coneType
        return when {
            expandedType.classId == SsrNames.modifier -> ParameterMappingResult.Skipped
            SlotParameterMapper.isSlot(parameter = parameter, session = session) ->
                ParameterMappingResult.Supported(
                    parameter = SlotParameterMapper.toSlot(
                        parameter = parameter,
                        session = session,
                        hasDefault = hasDefault,
                    ),
                )
            else -> mapValueParameter(
                parameter = parameter,
                expandedType = expandedType,
                session = session,
                hasDefault = hasDefault,
            )
        }
    }

    private fun mapValueParameter(
        parameter: FirValueParameter,
        expandedType: ConeKotlinType,
        session: FirSession,
        hasDefault: Boolean,
    ): ParameterMappingResult =
        when (val valueKind = valueKindOf(expandedType = expandedType, session = session)) {
            is ValueKindMapping.Supported -> ParameterMappingResult.Supported(
                parameter = Parameter.Value(
                    name = parameter.name.asString(),
                    kind = valueKind.kind,
                    required = !hasDefault,
                    nullable = expandedType.isMarkedNullable,
                ),
                choice = valueKind.choice,
            )
            is ValueKindMapping.Unsupported -> ParameterMappingResult.Unsupported(reason = valueKind.reason)
        }

    private fun valueKindOf(expandedType: ConeKotlinType, session: FirSession): ValueKindMapping =
        if (expandedType.classId == StandardClassIds.List) {
            listValueKindOf(listType = expandedType, session = session)
        } else {
            elementValueKindOf(
                expandedType = expandedType,
                session = session,
                unsupportedReason = UNSUPPORTED_TYPE_REASON,
            )
        }

    private fun listValueKindOf(listType: ConeKotlinType, session: FirSession): ValueKindMapping {
        val itemType = listType.typeArguments.singleOrNull()?.type?.fullyExpandedType(useSiteSession = session)
        return when {
            itemType == null -> ValueKindMapping.Unsupported(reason = UNSUPPORTED_LIST_ELEMENT_REASON)
            itemType.isMarkedNullable -> ValueKindMapping.Unsupported(reason = NULLABLE_LIST_ELEMENT_REASON)
            itemType.classId == StandardClassIds.List -> ValueKindMapping.Unsupported(reason = NESTED_LIST_REASON)
            else -> listElementKindOf(itemType = itemType, session = session)
        }
    }

    private fun listElementKindOf(itemType: ConeKotlinType, session: FirSession): ValueKindMapping =
        when (
            val element = elementValueKindOf(
                expandedType = itemType,
                session = session,
                unsupportedReason = UNSUPPORTED_LIST_ELEMENT_REASON,
            )
        ) {
            is ValueKindMapping.Supported -> ValueKindMapping.Supported(
                kind = ValueKind.ListOf(element = element.kind),
                choice = element.choice,
            )
            is ValueKindMapping.Unsupported -> element
        }

    private fun elementValueKindOf(
        expandedType: ConeKotlinType,
        session: FirSession,
        unsupportedReason: String,
    ): ValueKindMapping =
        when (expandedType.classId) {
            StandardClassIds.String -> ValueKindMapping.Supported(kind = ValueKind.Text)
            StandardClassIds.Int, StandardClassIds.Long -> ValueKindMapping.Supported(kind = ValueKind.Whole)
            StandardClassIds.Float, StandardClassIds.Double -> ValueKindMapping.Supported(kind = ValueKind.Decimal)
            StandardClassIds.Boolean -> ValueKindMapping.Supported(kind = ValueKind.Flag)
            else -> choiceValueKindOf(
                expandedType = expandedType,
                session = session,
                unsupportedReason = unsupportedReason,
            )
        }

    private fun choiceValueKindOf(
        expandedType: ConeKotlinType,
        session: FirSession,
        unsupportedReason: String,
    ): ValueKindMapping {
        val symbol = expandedType.toRegularClassSymbol(session = session)
            ?.takeIf { it.classKind == ClassKind.ENUM_CLASS }
            ?: return ValueKindMapping.Unsupported(reason = unsupportedReason)
        return choiceListOf(symbol = symbol, session = session)
    }

    private fun choiceListOf(symbol: FirRegularClassSymbol, session: FirSession): ValueKindMapping {
        val name = symbol.name.asString()
        val entries = symbol.collectEnumEntries(session = session).map { entry -> entry.name.asString() }
        return when {
            entries.isEmpty() -> ValueKindMapping.Unsupported(
                reason = "the enum $name has no entries, and a choice list must have at least one entry",
            )
            !isKotlinIdentifier(text = name) || !entries.all { entry -> isKotlinIdentifier(text = entry) } ->
                ValueKindMapping.Unsupported(
                    reason = "the enum $name or one of its entries is not a plain Kotlin identifier of at most " +
                        "${CatalogLimits.MAX_NAME_LENGTH} characters",
                )
            else -> ValueKindMapping.Supported(
                kind = ValueKind.Choice(listName = name),
                choice = ChoiceList(name = name, entries = entries),
            )
        }
    }

    private sealed interface ValueKindMapping {
        data class Supported(val kind: ValueKind, val choice: ChoiceList? = null) : ValueKindMapping
        data class Unsupported(val reason: String) : ValueKindMapping
    }
}
