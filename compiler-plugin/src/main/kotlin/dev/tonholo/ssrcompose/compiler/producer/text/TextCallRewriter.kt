@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package dev.tonholo.ssrcompose.compiler.producer.text

import dev.tonholo.ssrcompose.compiler.producer.ProducerNames
import dev.tonholo.ssrcompose.compiler.producer.diagnostic.TextDiagnostics
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.callableId
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

/**
 * Replaces each call to a foundation `BasicText` with a call to the `ScaledBasicText` that has the same parameter
 * types, passing every argument through in the same position. A call it cannot replace stays as it is and is
 * reported as a compile error.
 */
internal class TextCallRewriter(
    private val context: IrPluginContext,
    private val overloads: ScaledTextOverloads,
) : IrElementTransformerVoid() {
    private lateinit var currentFile: IrFile

    override fun visitFile(declaration: IrFile): IrFile {
        currentFile = declaration
        return super.visitFile(declaration)
    }

    override fun visitCall(expression: IrCall): IrExpression {
        val visited = super.visitCall(expression)
        val call = visited as? IrCall
        return if (call != null && call.symbol.owner.callableId == ProducerNames.basicText) {
            rewritten(call = call)
        } else {
            visited
        }
    }

    private fun rewritten(call: IrCall): IrCall {
        val replacement = overloads.matching(callee = call.symbol.owner)
        if (replacement == null) {
            reportNotSupported(call = call)
            return call
        }
        return call.replacedBy(replacement)
    }

    private fun IrCall.replacedBy(replacement: IrSimpleFunctionSymbol): IrCall {
        val scaled = IrCallImpl(
            startOffset = startOffset,
            endOffset = endOffset,
            type = type,
            symbol = replacement,
            origin = origin,
        )
        arguments.forEachIndexed { index, argument -> scaled.arguments[index] = argument }
        return scaled
    }

    private fun reportNotSupported(call: IrCall) {
        context.diagnosticReporter.at(call, currentFile).report(
            factory = TextDiagnostics.TEXT_OVERLOAD_NOT_SUPPORTED,
            a = TextOverloadMessage.notSupported(callee = call.symbol.owner),
        )
    }
}
