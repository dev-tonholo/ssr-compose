@file:OptIn(UnsafeDuringIrConstructionAPI::class)

package dev.tonholo.ssrcompose.compiler.producer.ir

import dev.tonholo.ssrcompose.compiler.producer.BridgeCatalog
import dev.tonholo.ssrcompose.compiler.producer.BridgeComponent
import dev.tonholo.ssrcompose.compiler.producer.BridgeKey
import dev.tonholo.ssrcompose.compiler.producer.BridgeParameter
import dev.tonholo.ssrcompose.compiler.producer.BridgeParameterKind
import dev.tonholo.ssrcompose.compiler.producer.ProducerNames
import dev.tonholo.ssrcompose.compiler.producer.UnplaceableComponent
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.IrStatementsBuilder
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irIfNull
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.builders.irVararg
import org.jetbrains.kotlin.ir.builders.typeOperator
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.types.makeNullable
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
    private val catalogVersion: IrConstructorSymbol by lazy {
        finder.findConstructors(ProducerNames.catalogVersion).single { it.owner.isPrimary }
    }
    private val textValue: IrConstructorSymbol by lazy {
        finder.findConstructors(ProducerNames.textValue).single { it.owner.isPrimary }
    }
    private val pair: IrClassSymbol by lazy { checkNotNull(finder.findClass(ProducerNames.pair)) }
    private val to: IrSimpleFunctionSymbol by lazy { finder.findFunctions(ProducerNames.to).single() }
    private val mapOf: IrSimpleFunctionSymbol by lazy {
        finder.findFunctions(ProducerNames.mapOf).single { it.owner.hasOnlyVarargParameter() }
    }
    private val emptyMap: IrSimpleFunctionSymbol by lazy { finder.findFunctions(ProducerNames.emptyMap).single() }
    private val error: IrSimpleFunctionSymbol by lazy { finder.findFunctions(ProducerNames.error).single() }

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        val key = (declaration.origin as? IrDeclarationOrigin.GeneratedByPlugin)?.pluginKey as? BridgeKey
        val catalog = key?.let(catalogsByKey::get)
        if (key != null && catalog != null && declaration.body == null) {
            val component = catalog.componentsByName.getValue(declaration.name.asString())
            declaration.defaultOptionalParametersToNull(component = component)
            val unplaceableMessage = UnplaceableComponent.messageOf(component)
            declaration.body = DeclarationIrBuilder(context, declaration.symbol).irBlockBody {
                if (unplaceableMessage == null) {
                    +consumerComponentCall(function = declaration, component = component, key = key)
                } else {
                    +errorCall(message = unplaceableMessage)
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

    private fun IrStatementsBuilder<*>.consumerComponentCall(
        function: IrSimpleFunction,
        component: BridgeComponent,
        key: BridgeKey,
    ): IrExpression {
        val parameters = function.parameters.associateBy { it.name.asString() }

        fun entriesOf(kind: BridgeParameterKind, value: (BridgeParameter, IrValueParameter) -> IrExpression) =
            component.parameters.filter { it.kind == kind }.map { parameter ->
                parameter.name to value(parameter, parameters.getValue(parameter.name))
            }

        val calleeParameters = consumerComponent.owner.parameters
        return irCall(consumerComponent).apply {
            arguments[CATALOG_VERSION_INDEX] = irCallConstructor(
                callee = catalogVersion,
                typeArguments = emptyList(),
            ).apply { arguments[0] = irString(key.catalogVersion) }
            arguments[NAME_INDEX] = irString(component.name)
            arguments[VALUES_INDEX] = mapCall(
                calleeParameter = calleeParameters[VALUES_INDEX],
                entries = entriesOf(kind = BridgeParameterKind.Text) { parameter, value ->
                    branchFreeTextValue(parameter = parameter, value = value)
                },
            )
            arguments[ACTIONS_INDEX] = mapCall(
                calleeParameter = calleeParameters[ACTIONS_INDEX],
                entries = entriesOf(kind = BridgeParameterKind.ActionOfText) { _, action -> irGet(action) },
            )
            arguments[SLOTS_INDEX] = mapCall(
                calleeParameter = calleeParameters[SLOTS_INDEX],
                entries = entriesOf(kind = BridgeParameterKind.Slot) { _, slot -> irGet(slot) },
            )
        }
    }

    /** Keeps an optional value's null check out of the `mapOf` arguments, where a branch costs a full stack frame. */
    private fun IrStatementsBuilder<*>.branchFreeTextValue(
        parameter: BridgeParameter,
        value: IrValueParameter,
    ): IrExpression {
        if (parameter.isRequired) return textValueCall(text = irGet(value))
        return irGet(irTemporary(optionalTextValue(value = value)))
    }

    private fun IrBuilderWithScope.optionalTextValue(value: IrValueParameter): IrExpression {
        val textType = value.type.makeNotNull()
        return irIfNull(
            type = textValue.owner.returnType.makeNullable(),
            subject = irGet(value),
            thenPart = irNull(),
            elsePart = textValueCall(
                text = typeOperator(
                    resultType = textType,
                    argument = irGet(value),
                    typeOperator = IrTypeOperator.IMPLICIT_NOTNULL,
                    typeOperand = textType,
                ),
            ),
        )
    }

    private fun IrBuilderWithScope.textValueCall(text: IrExpression): IrExpression =
        irCallConstructor(callee = textValue, typeArguments = emptyList()).apply { arguments[0] = text }

    private fun IrBuilderWithScope.errorCall(message: String): IrExpression =
        irCall(error).apply { arguments[0] = irString(message) }

    private fun IrBuilderWithScope.mapCall(
        calleeParameter: IrValueParameter,
        entries: List<Pair<String, IrExpression>>,
    ): IrExpression {
        val valueType = calleeParameter.mapValueType()
        val keyType = context.irBuiltIns.stringType
        val mapType = calleeParameter.type
        if (entries.isEmpty()) {
            return irCall(callee = emptyMap, type = mapType).apply {
                typeArguments[0] = keyType
                typeArguments[1] = valueType
            }
        }
        return irCall(callee = mapOf, type = mapType).apply {
            typeArguments[0] = keyType
            typeArguments[1] = valueType
            arguments[0] = irVararg(
                elementType = pair.typeWith(keyType, valueType),
                values = entries.map { (name, value) -> pairCall(valueType = valueType, name = name, value = value) },
            )
        }
    }

    private fun IrBuilderWithScope.pairCall(valueType: IrType, name: String, value: IrExpression): IrExpression =
        irCall(callee = to, type = pair.typeWith(context.irBuiltIns.stringType, valueType)).apply {
            typeArguments[0] = context.irBuiltIns.stringType
            typeArguments[1] = valueType
            arguments[0] = irString(name)
            arguments[1] = value
        }

    private fun IrSimpleFunction.hasOnlyVarargParameter(): Boolean =
        parameters.singleOrNull()?.varargElementType != null

    private fun IrValueParameter.mapValueType(): IrType =
        ((type as IrSimpleType).arguments[1] as IrTypeProjection).type

    private companion object {
        const val CATALOG_VERSION_INDEX = 0
        const val NAME_INDEX = 1
        const val VALUES_INDEX = 2
        const val ACTIONS_INDEX = 3
        const val SLOTS_INDEX = 4
    }
}
