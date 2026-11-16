package dev.tonholo.ssrcompose.catalog.model

import dev.tonholo.ssrcompose.catalog.validation.CatalogLimits
import dev.tonholo.ssrcompose.catalog.validation.requireIdentifier
import dev.tonholo.ssrcompose.core.data.ValueKind

/**
 * An input of a component. [required] is false when the composable gives the parameter a default value.
 * The default itself is not part of the catalog.
 *
 * Every case throws [IllegalArgumentException] when its name is not a Kotlin identifier or when it breaks
 * the rule written on it.
 */
public sealed interface Parameter {
    public val name: String
    public val required: Boolean

    /** A text, number, flag, choice or list. [nullable] is true when its type in the composable is nullable. */
    public data class Value(
        override val name: String,
        public val kind: ValueKind,
        override val required: Boolean,
        public val nullable: Boolean = false,
    ) : Parameter {
        init {
            requireParameterName(name = name)
            require(kind != ValueKind.Bytes) {
                "The parameter $name cannot hold bytes: bytes only go through a callback"
            }
        }
    }

    /** A callback that carries up to four arguments and returns nothing. */
    public data class Callback(
        override val name: String,
        public val arguments: List<ValueKind> = emptyList(),
        override val required: Boolean,
    ) : Parameter {
        init {
            requireParameterName(name = name)
            require(arguments.size <= CatalogLimits.MAX_CALLBACK_ARGUMENTS) {
                "The callback $name must have at most ${CatalogLimits.MAX_CALLBACK_ARGUMENTS} arguments"
            }
        }
    }

    /** A composable content slot. [arguments] are the values the component passes to the content. */
    public data class Slot(
        override val name: String,
        public val accepts: SlotAccepts = SlotAccepts.Any,
        public val arguments: List<ValueKind> = emptyList(),
        override val required: Boolean,
    ) : Parameter {
        init {
            requireParameterName(name = name)
            require(ValueKind.Bytes !in arguments) {
                "The slot $name cannot pass bytes: bytes only go through a callback"
            }
        }
    }
}

internal fun Parameter.choiceListNames(): Set<String> = when (this) {
    is Parameter.Value -> kind.choiceListNames()
    is Parameter.Callback -> arguments.flatMapTo(destination = mutableSetOf()) { it.choiceListNames() }
    is Parameter.Slot -> arguments.flatMapTo(destination = mutableSetOf()) { it.choiceListNames() }
}

private fun requireParameterName(name: String) = requireIdentifier(text = name, what = "A parameter name")
