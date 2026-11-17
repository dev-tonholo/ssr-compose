package dev.tonholo.ssrcompose.compiler.consumer

import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.compiler.consumer.slot.SlotParameterMapper
import dev.tonholo.ssrcompose.core.data.ValueKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isString

/**
 * Turns the parameter of a marked composable into a catalog parameter.
 */
internal object ParameterKindMapper {
    fun isSupported(parameter: FirValueParameter, session: FirSession): Boolean =
        parameter.returnTypeRef.coneType.isString ||
            SlotParameterMapper.isSlot(parameter = parameter, session = session)

    fun toParameter(parameter: FirValueParameter, session: FirSession): Parameter =
        if (SlotParameterMapper.isSlot(parameter = parameter, session = session)) {
            SlotParameterMapper.toSlot(parameter = parameter, session = session)
        } else {
            Parameter.Value(
                name = parameter.name.asString(),
                kind = ValueKind.Text,
                required = parameter.defaultValue == null,
            )
        }
}
