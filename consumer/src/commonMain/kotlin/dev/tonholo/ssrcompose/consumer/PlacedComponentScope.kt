package dev.tonholo.ssrcompose.consumer

import androidx.compose.runtime.Immutable
import dev.tonholo.ssrcompose.consumer.internal.read.KindName
import dev.tonholo.ssrcompose.consumer.internal.read.PlacedProblemReporter
import dev.tonholo.ssrcompose.consumer.internal.read.absent
import dev.tonholo.ssrcompose.consumer.internal.read.absentChoice
import dev.tonholo.ssrcompose.core.data.DataValue
import dev.tonholo.ssrcompose.core.message.PlacedComponent

private const val TEXT_STAND_IN = ""
private const val WHOLE_STAND_IN = 0L
private const val DECIMAL_STAND_IN = 0.0
private const val FLAG_STAND_IN = false

/**
 * What a binding receives: a placed component, read by parameter name.
 *
 * A read on this class throws [PlacedValueException] when the value is absent or `None`.
 * The `OrNull` read of each kind is an extension function in this package. It returns null in those two cases.
 * Both throw [PlacedValueException] when the value has another kind than the read asks for.
 *
 * Inside `SsrScreen` a failed read does not throw, because an exception during composition stops the app's
 * recomposer. It reports the problem and returns a stand-in: an empty text or list, zero, false, or the first entry
 * of the enum. `SsrScreen` never draws content with a reported problem and shows the problem instead.
 * Content in a `Dialog` or `Popup` of a failing binding has its own window and can show the stand-in briefly,
 * until the problem replaces the screen (2 draws measured in a `Dialog`, 1 in a `Popup`). #189 removes this limit.
 * A choice read on an enum with no entries still throws, because no stand-in exists.
 *
 * Two scopes are equal when they wrap equal placed components,
 * so a binding is skipped when its component did not change.
 * The placed component and its maps and lists must not change after the scope is made.
 */
@Immutable
public class PlacedComponentScope internal constructor(
    internal val placed: PlacedComponent,
    internal val problems: PlacedProblemReporter? = null,
) {
    /**
     * The text of the parameter [name].
     *
     * @throws PlacedValueException when the value is absent, `None`, or not a text.
     */
    public fun text(name: String): String =
        textOrNull(name = name) ?: absent(name = name, expected = KindName.TEXT, standIn = TEXT_STAND_IN)

    /**
     * The whole number of the parameter [name], as a [Long].
     *
     * @throws PlacedValueException when the value is absent, `None`, or not a whole number.
     */
    public fun whole(name: String): Long =
        wholeOrNull(name = name) ?: absent(name = name, expected = KindName.WHOLE, standIn = WHOLE_STAND_IN)

    /**
     * The decimal number of the parameter [name], as a [Double].
     *
     * @throws PlacedValueException when the value is absent, `None`, or not a decimal number.
     */
    public fun decimal(name: String): Double =
        decimalOrNull(name = name) ?: absent(name = name, expected = KindName.DECIMAL, standIn = DECIMAL_STAND_IN)

    /**
     * The flag of the parameter [name].
     *
     * @throws PlacedValueException when the value is absent, `None`, or not a flag.
     */
    public fun flag(name: String): Boolean =
        flagOrNull(name = name) ?: absent(name = name, expected = KindName.FLAG, standIn = FLAG_STAND_IN)

    /**
     * The entry of the choice list the parameter [name] holds, as the enum [E].
     *
     * @throws PlacedValueException when the value is absent, `None`, not a choice, or an entry that [E] does not have.
     */
    public inline fun <reified E : Enum<E>> choice(name: String): E =
        choiceOrNull<E>(name = name) ?: absentChoice(name = name, entries = enumValues<E>())

    /**
     * The items of the list the parameter [name] holds, as core's [DataValue]s.
     *
     * @throws PlacedValueException when the value is absent, `None`, or not a list.
     */
    public fun list(name: String): List<DataValue> =
        listOrNull(name = name) ?: absent(name = name, expected = KindName.LIST, standIn = emptyList())

    override fun equals(other: Any?): Boolean = other is PlacedComponentScope && other.placed == placed

    override fun hashCode(): Int = placed.hashCode()
}
