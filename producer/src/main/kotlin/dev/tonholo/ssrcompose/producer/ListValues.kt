package dev.tonholo.ssrcompose.producer

import dev.tonholo.ssrcompose.core.annotation.InternalSsrComposeApi
import dev.tonholo.ssrcompose.core.data.DataValue

/** Turns a list of texts into a [DataValue.ListValue] of [DataValue.Text], keeping their order. */
@InternalSsrComposeApi
public fun textListValue(items: List<String>): DataValue.ListValue =
    DataValue.ListValue(items = items.map(DataValue::Text))

/** Turns a list of whole numbers into a [DataValue.ListValue] of [DataValue.WholeNumber], keeping their order. */
@InternalSsrComposeApi
public fun wholeListValue(items: List<Long>): DataValue.ListValue =
    DataValue.ListValue(items = items.map(DataValue::WholeNumber))

/** Turns a list of decimal numbers into a [DataValue.ListValue] of [DataValue.DecimalNumber], keeping their order. */
@InternalSsrComposeApi
public fun decimalListValue(items: List<Double>): DataValue.ListValue =
    DataValue.ListValue(items = items.map(DataValue::DecimalNumber))

/** Turns a list of flags into a [DataValue.ListValue] of [DataValue.Flag], keeping their order. */
@InternalSsrComposeApi
public fun flagListValue(items: List<Boolean>): DataValue.ListValue =
    DataValue.ListValue(items = items.map(DataValue::Flag))

/** Turns a list of choice entries into a [DataValue.ListValue] of [DataValue.Choice] by entry name, keeping order. */
@InternalSsrComposeApi
public fun choiceListValue(items: List<Enum<*>>): DataValue.ListValue =
    DataValue.ListValue(items = items.map { entry -> DataValue.Choice(entry = entry.name) })
