package dev.tonholo.ssrcompose.catalog.model

import dev.tonholo.ssrcompose.catalog.validation.requireIdentifier

/**
 * A marked composable and its parameters, in declaration order.
 *
 * There is no limit on the number of parameters.
 *
 * @throws IllegalArgumentException when the name is not a Kotlin identifier or a parameter name is repeated.
 */
public data class ComponentDescriptor(public val name: String, public val parameters: List<Parameter>) {
    init {
        requireIdentifier(text = name, what = "A component name")
        require(parameters.map { it.name }.toSet().size == parameters.size) {
            "The component $name must not repeat a parameter name"
        }
    }
}
