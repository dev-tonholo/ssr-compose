@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package dev.tonholo.ssrcompose.compiler.producer.text

import dev.tonholo.ssrcompose.compiler.producer.ProducerNames
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrType

/** The overloads of the producer's `ScaledBasicText`, which stand in for the overloads of `BasicText`. */
internal class ScaledTextOverloads(context: IrPluginContext) {
    private val finder = context.finderForBuiltins()
    private val overloads: List<IrSimpleFunctionSymbol> by lazy {
        finder.findFunctions(ProducerNames.scaledBasicText).toList()
    }

    /** False when the producer library is not on the class path, so there is nothing to rewrite to. */
    val isAvailable: Boolean get() = overloads.isNotEmpty()

    /** The overload with the parameter types of [callee], or null when none has them. */
    fun matching(callee: IrSimpleFunction): IrSimpleFunctionSymbol? {
        val wanted = callee.parameterTypes()
        return overloads.firstOrNull { overload -> overload.owner.parameterTypes() == wanted }
    }

    private fun IrSimpleFunction.parameterTypes(): List<IrType> = parameters.map { parameter -> parameter.type }
}
