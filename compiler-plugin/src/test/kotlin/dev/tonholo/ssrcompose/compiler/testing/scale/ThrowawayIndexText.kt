package dev.tonholo.ssrcompose.compiler.testing.scale

import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.Parameter

/** Writes components in the index format the compiler plugin reads until T031a replaces it. */
internal object ThrowawayIndexText {
    fun of(components: List<ComponentDescriptor>): String = buildString {
        components.forEach { component ->
            appendLine("component ${component.name}")
            component.parameters.forEach { parameter ->
                appendLine("param ${parameter.name} ${kindOf(parameter)} ${requirementOf(parameter)}")
            }
        }
    }

    private fun kindOf(parameter: Parameter): String = when (parameter) {
        is Parameter.Value -> "text"
        is Parameter.Callback -> "action-of-text"
        is Parameter.Slot -> "slot"
    }

    private fun requirementOf(parameter: Parameter): String = if (parameter.required) "required" else "optional"
}
