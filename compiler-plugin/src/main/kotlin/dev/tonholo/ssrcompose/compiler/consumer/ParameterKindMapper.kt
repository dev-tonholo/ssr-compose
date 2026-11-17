package dev.tonholo.ssrcompose.compiler.consumer

import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.core.data.ValueKind
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isString

/**
 * Turns the parameter of a marked composable into a catalog parameter. Only text is supported so far.
 */
internal object ParameterKindMapper {
    fun isSupported(parameter: FirValueParameter): Boolean = parameter.returnTypeRef.coneType.isString

    fun toParameter(parameter: FirValueParameter): Parameter =
        Parameter.Value(
            name = parameter.name.asString(),
            kind = ValueKind.Text,
            required = parameter.defaultValue == null,
        )
}
