package dev.tonholo.ssrcompose.consumer

import androidx.compose.runtime.Immutable
import dev.tonholo.ssrcompose.core.data.DataValue
import dev.tonholo.ssrcompose.core.message.PlacedComponent

/**
 * Stand-in for `:consumer`'s `PlacedComponentScope` so `:compiler-plugin` JVM compile tests can compile and run
 * created consumer renderers without depending on the Android-only `:consumer` module.
 */
@Immutable
class PlacedComponentScope(val placed: PlacedComponent) {
    /** Reads the text value of [name], or fails when it is absent or not text. */
    fun text(name: String): String {
        val value = placed.values[name] as? DataValue.Text
        return checkNotNull(value) { "${placed.name}.$name needs a text value" }.value
    }
}
