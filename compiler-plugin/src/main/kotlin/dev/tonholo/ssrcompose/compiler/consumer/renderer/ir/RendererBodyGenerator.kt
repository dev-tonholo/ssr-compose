@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package dev.tonholo.ssrcompose.compiler.consumer.renderer.ir

import dev.tonholo.ssrcompose.compiler.SsrNames
import dev.tonholo.ssrcompose.compiler.consumer.renderer.RendererKey
import dev.tonholo.ssrcompose.compiler.consumer.renderer.RendererNames
import dev.tonholo.ssrcompose.compiler.consumer.renderer.RendererParameter
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.FqName

/**
 * Builds the body of each created consumer renderer: one call to the marked composable with each text parameter
 * decoded through `PlacedComponentScope.text`.
 */
internal class RendererBodyGenerator(
    module: IrModuleFragment,
    private val context: IrPluginContext,
) : IrElementTransformerVoid() {
    private val composablesByKey: Map<RendererKey, IrSimpleFunctionSymbol> =
        module.files.flatMap { file ->
            file.declarations
                .filterIsInstance<IrSimpleFunction>()
                .filter(::isMarkedComposable)
                .mapNotNull { function ->
                    rendererKeyOrNull(packageFqName = file.packageFqName, function = function)
                        ?.let { key -> key to function.symbol }
                }
        }.toMap()
    private val finder = context.finderForBuiltins()
    private val textRead: IrSimpleFunctionSymbol by lazy {
        checkNotNull(finder.findFunctions(callableId = RendererNames.text).singleOrNull()) {
            "The consumer library function ${RendererNames.text} is not on the classpath"
        }
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        val key = (declaration.origin as? IrDeclarationOrigin.GeneratedByPlugin)?.pluginKey as? RendererKey
        if (key != null && declaration.body == null) {
            val target = targetComposable(key = key)
            val placedParameter = placedParameterOf(renderer = declaration)
            declaration.body = DeclarationIrBuilder(
                generatorContext = context,
                symbol = declaration.symbol,
            ).irBlockBody {
                +composableCall(target = target, placedParameter = placedParameter)
            }
        }
        return super.visitSimpleFunction(declaration)
    }

    private fun isMarkedComposable(function: IrSimpleFunction): Boolean =
        !function.isExpect &&
            function.typeParameters.isEmpty() &&
            function.visibility != DescriptorVisibilities.PRIVATE &&
            function.hasAnnotation(classId = SsrNames.consumerComposable) &&
            function.parameters.all { parameter -> parameter.kind == IrParameterKind.Regular }

    private fun rendererKeyOrNull(packageFqName: FqName, function: IrSimpleFunction): RendererKey? {
        val parameters = function.parameters.map { parameter ->
            val type = parameter.type.classOrNull?.owner?.classId ?: return null
            RendererParameter(name = parameter.name, type = type)
        }
        return RendererKey(
            composable = CallableId(packageName = packageFqName, callableName = function.name),
            parameters = parameters,
        )
    }

    private fun targetComposable(key: RendererKey): IrSimpleFunctionSymbol =
        checkNotNull(composablesByKey[key]) {
            "The marked composable ${key.composable} with parameters ${key.parameters} was not found in the module"
        }

    private fun placedParameterOf(renderer: IrSimpleFunction): IrValueParameter =
        checkNotNull(
            renderer.parameters.singleOrNull { parameter -> parameter.name == RendererNames.placedParameter },
        ) {
            "The created renderer ${renderer.name} has no '${RendererNames.placedParameter}' parameter"
        }

    private fun IrBuilderWithScope.composableCall(
        target: IrSimpleFunctionSymbol,
        placedParameter: IrValueParameter,
    ): IrExpression =
        irCall(callee = target).apply {
            target.owner.parameters
                .filter { parameter -> parameter.kind == IrParameterKind.Regular }
                .forEach { parameter ->
                    arguments[parameter.indexInParameters] = textReadCall(
                        placedParameter = placedParameter,
                        parameterName = parameter.name.asString(),
                    )
                }
        }

    private fun IrBuilderWithScope.textReadCall(
        placedParameter: IrValueParameter,
        parameterName: String,
    ): IrExpression =
        irCall(callee = textRead).apply {
            arguments[RECEIVER_INDEX] = irGet(variable = placedParameter)
            arguments[PARAMETER_NAME_INDEX] = irString(value = parameterName)
        }

    private companion object {
        const val RECEIVER_INDEX = 0
        const val PARAMETER_NAME_INDEX = 1
    }
}
