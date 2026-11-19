package dev.tonholo.ssrcompose.consumer

import dev.tonholo.ssrcompose.consumer.internal.read.KindName
import dev.tonholo.ssrcompose.consumer.internal.read.choiceEntryOrNull
import dev.tonholo.ssrcompose.consumer.internal.read.entryOf
import dev.tonholo.ssrcompose.consumer.internal.read.valueOrNull
import dev.tonholo.ssrcompose.core.data.DataValue

/**
 * The text of the parameter [name], or null when it is absent or `None`.
 *
 * @throws PlacedValueException when the value is not a text.
 */
public fun PlacedComponentScope.textOrNull(name: String): String? =
    valueOrNull<DataValue.Text>(name = name, expected = KindName.TEXT)?.value

/**
 * The whole number of the parameter [name], or null when it is absent or `None`.
 *
 * @throws PlacedValueException when the value is not a whole number.
 */
public fun PlacedComponentScope.wholeOrNull(name: String): Long? =
    valueOrNull<DataValue.WholeNumber>(name = name, expected = KindName.WHOLE)?.value

/**
 * The decimal number of the parameter [name], or null when it is absent or `None`.
 *
 * @throws PlacedValueException when the value is not a decimal number.
 */
public fun PlacedComponentScope.decimalOrNull(name: String): Double? =
    valueOrNull<DataValue.DecimalNumber>(name = name, expected = KindName.DECIMAL)?.value

/**
 * The flag of the parameter [name], or null when it is absent or `None`.
 *
 * @throws PlacedValueException when the value is not a flag.
 */
public fun PlacedComponentScope.flagOrNull(name: String): Boolean? =
    valueOrNull<DataValue.Flag>(name = name, expected = KindName.FLAG)?.value

/**
 * The entry of the choice list the parameter [name] holds, as the enum [E], or null when it is absent or `None`.
 *
 * @throws PlacedValueException when the value is not a choice, or an entry that [E] does not have.
 */
public inline fun <reified E : Enum<E>> PlacedComponentScope.choiceOrNull(name: String): E? =
    choiceEntryOrNull(name = name)?.let { entry ->
        entryOf(name = name, entry = entry, entries = enumValues<E>(), enumName = E::class.simpleName.orEmpty())
    }

/**
 * The items of the list the parameter [name] holds, or null when it is absent or `None`.
 *
 * @throws PlacedValueException when the value is not a list.
 */
public fun PlacedComponentScope.listOrNull(name: String): List<DataValue>? =
    valueOrNull<DataValue.ListValue>(name = name, expected = KindName.LIST)?.items
