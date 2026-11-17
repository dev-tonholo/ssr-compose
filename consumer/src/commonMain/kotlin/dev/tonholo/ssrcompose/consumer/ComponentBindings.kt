package dev.tonholo.ssrcompose.consumer

import androidx.compose.runtime.Composable

/**
 * Hand-written renderers for components, by the name the screen places them with.
 *
 * Build one with `ComponentBindings { bind("Greeting") { placed -> Greeting(title = placed.text("title")) } }`.
 */
public class ComponentBindings private constructor(
    private val bindings: Map<String, @Composable (PlacedComponentScope) -> Unit>,
) {
    internal fun find(name: String): (@Composable (PlacedComponentScope) -> Unit)? = bindings[name]

    /** Collects the bindings of one [ComponentBindings]. */
    public class Builder internal constructor() {
        private val bindings = mutableMapOf<String, @Composable (PlacedComponentScope) -> Unit>()

        /**
         * Binds [content] to the component called [name].
         *
         * @throws IllegalArgumentException when [name] is already bound.
         */
        public fun bind(name: String, content: @Composable (PlacedComponentScope) -> Unit) {
            require(name !in bindings) { "The component $name is already bound" }
            bindings[name] = content
        }

        internal fun build(): ComponentBindings = ComponentBindings(bindings = bindings.toMap())
    }

    public companion object {
        /** Creates the bindings that [configure] declares. */
        public operator fun invoke(configure: Builder.() -> Unit): ComponentBindings =
            Builder().apply(configure).build()
    }
}
