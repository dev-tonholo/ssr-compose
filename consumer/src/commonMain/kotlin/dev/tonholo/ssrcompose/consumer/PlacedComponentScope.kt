package dev.tonholo.ssrcompose.consumer

import androidx.compose.runtime.Immutable
import dev.tonholo.ssrcompose.consumer.internal.KindName
import dev.tonholo.ssrcompose.consumer.internal.absent
import dev.tonholo.ssrcompose.consumer.internal.absentChoice
import dev.tonholo.ssrcompose.core.data.DataValue
import dev.tonholo.ssrcompose.core.message.PlacedComponent

/**
 * What a binding receives: a placed component, read by parameter name.
 *
 * A read on this class throws [PlacedValueException] when the value is absent or `None`.
 * The `OrNull` read of each kind is an extension function in this package. It returns null in those two cases.
 * Both throw [PlacedValueException] when the value has another kind than the read asks for.
 *
 * Two scopes are equal when they wrap equal placed components,
 * so a binding is skipped when its component did not change.
 * The placed component and its maps and lists must not change after the scope is made.
 */
@Immutable
public class PlacedComponentScope internal constructor(internal val placed: PlacedComponent) {
    /**
     * The text of the parameter [name].
     *
     * @throws PlacedValueException when the value is absent, `None`, or not a text.
     */
    public fun text(name: String): String = textOrNull(name = name) ?: absent(name = name, expected = KindName.TEXT)

    /**
     * The whole number of the parameter [name], as a [Long].
     *
     * @throws PlacedValueException when the value is absent, `None`, or not a whole number.
     */
    public fun whole(name: String): Long = wholeOrNull(name = name) ?: absent(name = name, expected = KindName.WHOLE)

    /**
     * The decimal number of the parameter [name], as a [Double].
     *
     * @throws PlacedValueException when the value is absent, `None`, or not a decimal number.
     */
    public fun decimal(name: String): Double =
        decimalOrNull(name = name) ?: absent(name = name, expected = KindName.DECIMAL)

    /**
     * The flag of the parameter [name].
     *
     * @throws PlacedValueException when the value is absent, `None`, or not a flag.
     */
    public fun flag(name: String): Boolean = flagOrNull(name = name) ?: absent(name = name, expected = KindName.FLAG)

    /**
     * The entry of the choice list the parameter [name] holds, as the enum [E].
     *
     * @throws PlacedValueException when the value is absent, `None`, not a choice, or an entry that [E] does not have.
     */
    public inline fun <reified E : Enum<E>> choice(name: String): E =
        choiceOrNull<E>(name = name) ?: absentChoice(name = name)

    /**
     * The items of the list the parameter [name] holds, as core's [DataValue]s.
     *
     * @throws PlacedValueException when the value is absent, `None`, or not a list.
     */
    public fun list(name: String): List<DataValue> =
        listOrNull(name = name) ?: absent(name = name, expected = KindName.LIST)

    override fun equals(other: Any?): Boolean = other is PlacedComponentScope && other.placed == placed

    override fun hashCode(): Int = placed.hashCode()
}
