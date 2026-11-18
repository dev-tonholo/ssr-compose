@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package dev.tonholo.ssrcompose.compiler.producer.ir

import dev.tonholo.ssrcompose.compiler.producer.BridgeCatalog
import dev.tonholo.ssrcompose.compiler.producer.BridgeComponent
import dev.tonholo.ssrcompose.compiler.producer.BridgeKey
import dev.tonholo.ssrcompose.compiler.producer.BridgeParameterKind
import dev.tonholo.ssrcompose.compiler.producer.ProducerNames
import dev.tonholo.ssrcompose.compiler.producer.UnplaceableComponent
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irVararg
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

internal class BridgeBodyGenerator(
    catalogs: List<BridgeCatalog>,
    private val context: IrPluginContext,
) : IrElementTransformerVoid() {
    private val catalogsByKey: Map<BridgeKey, BridgeCatalog> = catalogs.associateBy { it.key }
    private val finder = context.finderForBuiltins()
    private val consumerComponent: IrSimpleFunctionSymbol by lazy {
        checkNotNull(finder.findFunctions(ProducerNames.consumerComponent).singleOrNull()) {
            "The producer library function ${ProducerNames.consumerComponent} is not on the classpath"
        }
    }
    private val arrayOf: IrSimpleFunctionSymbol by lazy { finder.findFunctions(ProducerNames.arrayOf).single() }
    private val error: IrSimpleFunctionSymbol by lazy { finder.findFunctions(ProducerNames.error).single() }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        val key = (declaration.origin as? IrDeclarationOrigin.GeneratedByPlugin)?.pluginKey as? BridgeKey
        val catalog = key?.let(catalogsByKey::get)
        if (key != null && catalog != null && declaration.body == null) {
            val component = catalog.componentsByName.getValue(declaration.name.asString())
            declaration.defaultOptionalParametersToNull(component = component)
            val unplaceableMessage = UnplaceableComponent.messageOf(component)
            declaration.body = DeclarationIrBuilder(context, declaration.symbol).irBlockBody {
                +if (unplaceableMessage == null) {
                    consumerComponentCall(function = declaration, component = component, key = key)
                } else {
                    errorCall(message = unplaceableMessage)
                }
            }
        }
        return super.visitSimpleFunction(declaration)
    }

    private fun IrSimpleFunction.defaultOptionalParametersToNull(component: BridgeComponent) {
        val builder = DeclarationIrBuilder(context, symbol)
        val optionalNames = component.parameters.filterNot { it.isRequired }.map { it.name }.toSet()
        parameters.filter { it.name.asString() in optionalNames }.forEach { parameter ->
            parameter.defaultValue = builder.irExprBody(builder.irNull())
        }
    }

    private fun IrBuilderWithScope.consumerComponentCall(
        function: IrSimpleFunction,
        component: BridgeComponent,
        key: BridgeKey,
    ): IrExpression {
        val parameters = function.parameters.associateBy { it.name.asString() }

        fun parametersOf(kind: BridgeParameterKind): List<IrValueParameter> =
            component.parameters.filter { it.kind == kind }.map { parameters.getValue(it.name) }

        val values = parametersOf(kind = BridgeParameterKind.Text)
        val actions = parametersOf(kind = BridgeParameterKind.ActionOfText)
        val slots = parametersOf(kind = BridgeParameterKind.Slot)
        val calleeParameters = consumerComponent.owner.parameters
        return irCall(consumerComponent).apply {
            arguments[CATALOG_VERSION_INDEX] = irString(key.catalogVersion)
            arguments[NAME_INDEX] = irString(component.name)
            arguments[VALUE_NAMES_INDEX] = namesArray(parameters = values)
            arguments[VALUES_INDEX] = argumentsArray(
                calleeParameter = calleeParameters[VALUES_INDEX],
                parameters = values,
            )
            arguments[ACTION_NAMES_INDEX] = namesArray(parameters = actions)
            arguments[ACTIONS_INDEX] = argumentsArray(
                calleeParameter = calleeParameters[ACTIONS_INDEX],
                parameters = actions,
            )
            arguments[SLOT_NAMES_INDEX] = namesArray(parameters = slots)
            arguments[SLOTS_INDEX] = argumentsArray(
                calleeParameter = calleeParameters[SLOTS_INDEX],
                parameters = slots,
            )
        }
    }

    private fun IrBuilderWithScope.errorCall(message: String): IrExpression =
        irCall(error).apply { arguments[0] = irString(message) }

    private fun IrBuilderWithScope.argumentsArray(
        calleeParameter: IrValueParameter,
        parameters: List<IrValueParameter>,
    ): IrExpression = arrayOfCall(elementType = calleeParameter.elementType(), elements = parameters.map { irGet(it) })

    private fun IrBuilderWithScope.namesArray(parameters: List<IrValueParameter>): IrExpression =
        arrayOfCall(
            elementType = context.irBuiltIns.stringType,
            elements = parameters.map { irString(it.name.asString()) },
        )

    private fun IrBuilderWithScope.arrayOfCall(elementType: IrType, elements: List<IrExpression>): IrExpression =
        irCall(arrayOf, context.irBuiltIns.arrayClass.typeWith(elementType)).apply {
            typeArguments[0] = elementType
            arguments[0] = irVararg(elementType, elements)
        }

    private fun IrValueParameter.elementType(): IrType =
        ((type as IrSimpleType).arguments.single() as IrTypeProjection).type

    private companion object {
        const val CATALOG_VERSION_INDEX = 0
        const val NAME_INDEX = 1
        const val VALUE_NAMES_INDEX = 2
        const val VALUES_INDEX = 3
        const val ACTION_NAMES_INDEX = 4
        const val ACTIONS_INDEX = 5
        const val SLOT_NAMES_INDEX = 6
        const val SLOTS_INDEX = 7
    }
}
