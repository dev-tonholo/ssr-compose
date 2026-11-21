package dev.tonholo.ssrcompose.compiler.producer.text

import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.types.isMarkedNullable

internal object TextOverloadMessage {
    /** The error for a `BasicText` overload that `ScaledBasicText` has no copy of. */
    fun notSupported(callee: IrSimpleFunction): String {
        val parameters = callee.parameters.joinToString(separator = ", ") { parameter ->
            "${parameter.name.asString()}: ${parameter.type.shortName()}"
        }
        return "The overload 'BasicText($parameters)' is not supported yet: the producer library's " +
            "ScaledBasicText has no overload with these parameter types, so the text cannot follow the device's font scale."
    }

    private fun IrType.shortName(): String {
        val name = classFqName?.shortName()?.asString() ?: "?"
        return if (isMarkedNullable()) "$name?" else name
    }
}
