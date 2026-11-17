package dev.tonholo.ssrcompose.consumer

import dev.tonholo.ssrcompose.consumer.internal.KindName
import dev.tonholo.ssrcompose.consumer.internal.absentValue
import dev.tonholo.ssrcompose.consumer.internal.wrongKind
import dev.tonholo.ssrcompose.core.data.DataValue
import dev.tonholo.ssrcompose.core.message.PlacedComponent

/**
 * What a binding receives: a placed component, read by parameter name.
 *
 * A read without `OrNull` throws [PlacedValueException] when the value is absent or `None`.
 * A read with `OrNull` returns null in those two cases.
 * Both throw [PlacedValueException] when the value has another kind than the read asks for.
 */
public class PlacedComponentScope internal constructor(internal val placed: PlacedComponent) {
    /**
     * The text of the parameter [name].
     *
     * @throws PlacedValueException when the value is absent, `None`, or not a text.
     */
    public fun text(name: String): String = textOrNull(name = name) ?: absent(name = name, expected = KindName.TEXT)

    /**
     * The text of the parameter [name], or null when it is absent or `None`.
     *
     * @throws PlacedValueException when the value is not a text.
     */
    public fun textOrNull(name: String): String? =
        valueOrNull<DataValue.Text>(name = name, expected = KindName.TEXT)?.value

    private fun absent(name: String, expected: String): Nothing =
        throw absentValue(component = placed.name, parameter = name, expected = expected)

    private inline fun <reified V : DataValue> valueOrNull(name: String, expected: String): V? =
        when (val value = placed.values[name]) {
            null, DataValue.None -> null
            is V -> value
            else -> throw wrongKind(component = placed.name, parameter = name, expected = expected, found = value)
        }
}
