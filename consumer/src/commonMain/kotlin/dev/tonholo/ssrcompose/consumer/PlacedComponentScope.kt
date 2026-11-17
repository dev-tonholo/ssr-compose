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

    /**
     * The whole number of the parameter [name], as a [Long].
     *
     * @throws PlacedValueException when the value is absent, `None`, or not a whole number.
     */
    public fun whole(name: String): Long = wholeOrNull(name = name) ?: absent(name = name, expected = KindName.WHOLE)

    /**
     * The whole number of the parameter [name], or null when it is absent or `None`.
     *
     * @throws PlacedValueException when the value is not a whole number.
     */
    public fun wholeOrNull(name: String): Long? =
        valueOrNull<DataValue.WholeNumber>(name = name, expected = KindName.WHOLE)?.value

    /**
     * The decimal number of the parameter [name], as a [Double].
     *
     * @throws PlacedValueException when the value is absent, `None`, or not a decimal number.
     */
    public fun decimal(name: String): Double =
        decimalOrNull(name = name) ?: absent(name = name, expected = KindName.DECIMAL)

    /**
     * The decimal number of the parameter [name], or null when it is absent or `None`.
     *
     * @throws PlacedValueException when the value is not a decimal number.
     */
    public fun decimalOrNull(name: String): Double? =
        valueOrNull<DataValue.DecimalNumber>(name = name, expected = KindName.DECIMAL)?.value

    /**
     * The flag of the parameter [name].
     *
     * @throws PlacedValueException when the value is absent, `None`, or not a flag.
     */
    public fun flag(name: String): Boolean = flagOrNull(name = name) ?: absent(name = name, expected = KindName.FLAG)

    /**
     * The flag of the parameter [name], or null when it is absent or `None`.
     *
     * @throws PlacedValueException when the value is not a flag.
     */
    public fun flagOrNull(name: String): Boolean? =
        valueOrNull<DataValue.Flag>(name = name, expected = KindName.FLAG)?.value

    private fun absent(name: String, expected: String): Nothing =
        throw absentValue(component = placed.name, parameter = name, expected = expected)

    private inline fun <reified V : DataValue> valueOrNull(name: String, expected: String): V? =
        when (val value = placed.values[name]) {
            null, DataValue.None -> null
            is V -> value
            else -> throw wrongKind(component = placed.name, parameter = name, expected = expected, found = value)
        }
}
