package dev.tonholo.ssrcompose.compiler.producer

import dev.tonholo.ssrcompose.compiler.producer.throwaway.ThrowawayComponent
import dev.tonholo.ssrcompose.compiler.producer.throwaway.ThrowawayParameter
import dev.tonholo.ssrcompose.compiler.producer.throwaway.ThrowawayParameterKind

/** A composable is unplaceable when it has a required slot whose content receives arguments. */
internal object UnplaceableComponent {
    /** Returns the error for [component], or null when the server can place it. */
    fun messageOf(component: ThrowawayComponent): String? =
        component.parameters.firstOrNull { it.isRequiredSlotWithArguments() }?.let { slot ->
            "The composable '${component.name}' cannot be placed from the server: its required slot " +
                "'${slot.name}' receives arguments, and slots with arguments are not supported yet."
        }

    private fun ThrowawayParameter.isRequiredSlotWithArguments(): Boolean =
        isRequired && kind == ThrowawayParameterKind.SlotOfText
}
