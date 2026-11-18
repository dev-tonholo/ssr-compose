package dev.tonholo.ssrcompose.compiler.producer

/** A composable is unplaceable when it has a required slot whose content receives arguments. */
internal object UnplaceableComponent {
    /** Returns the error for [component], or null when the server can place it. */
    fun messageOf(component: BridgeComponent): String? =
        component.parameters.firstOrNull { it.isRequiredSlotWithArguments() }?.let { slot ->
            "The composable '${component.name}' cannot be placed from the server: its required slot " +
                "'${slot.name}' receives arguments, and slots with arguments are not supported yet."
        }

    private fun BridgeParameter.isRequiredSlotWithArguments(): Boolean =
        isRequired && kind == BridgeParameterKind.SlotOfText
}
