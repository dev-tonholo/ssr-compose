package dev.tonholo.ssrcompose.compiler.producer

import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.core.data.ValueKind

private val TEXT_ONLY = listOf(ValueKind.Text)

/** Returns the shape a bridge function gives this parameter, or null when the plugin cannot create it yet. */
internal fun Parameter.toBridgeParameter(): BridgeParameter? =
    bridgeKind()?.let { kind -> BridgeParameter(name = name, kind = kind, isRequired = required) }

private fun Parameter.bridgeKind(): BridgeParameterKind? = when (this) {
    is Parameter.Value -> BridgeParameterKind.Text.takeIf { kind == ValueKind.Text && !(required && nullable) }
    is Parameter.Callback -> BridgeParameterKind.ActionOfText.takeIf { required && arguments == TEXT_ONLY }
    is Parameter.Slot -> when {
        arguments.isEmpty() -> BridgeParameterKind.Slot
        required && arguments == TEXT_ONLY -> BridgeParameterKind.SlotOfText
        else -> null
    }
}
