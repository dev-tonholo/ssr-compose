package dev.tonholo.ssrcompose.catalog.bundle

import dev.tonholo.ssrcompose.catalog.model.CatalogPart
import dev.tonholo.ssrcompose.catalog.model.ChoiceList
import dev.tonholo.ssrcompose.catalog.model.ComponentDescriptor
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.catalog.model.PartName
import dev.tonholo.ssrcompose.catalog.model.SlotAccepts
import dev.tonholo.ssrcompose.core.data.ValueKind

internal const val EVERY_KIND_PART_RESOURCE = "golden/every-kind.part.json"

private val styleChoice = ValueKind.Choice(listName = "Style")

internal fun everyKindPart(): CatalogPart = CatalogPart(
    name = PartName(value = "every-kind"),
    choices = listOf(
        ChoiceList(name = "Size", entries = listOf("small")),
        ChoiceList(name = "Style", entries = listOf("titleLarge", "bodyMedium")),
    ),
    components = listOf(
        ComponentDescriptor(name = "Callbacks", parameters = callbackParameters()),
        ComponentDescriptor(name = "Empty", parameters = emptyList()),
        ComponentDescriptor(name = "Slots", parameters = slotParameters()),
        ComponentDescriptor(name = "Values", parameters = valueParameters()),
    ),
)

internal fun everyKindPartInAnotherOrder(): CatalogPart = everyKindPart().let { part ->
    part.copy(choices = part.choices.reversed(), components = part.components.reversed())
}

private fun callbackParameters(): List<Parameter> = listOf(
    Parameter.Callback(name = "onClick", required = true),
    Parameter.Callback(
        name = "onPick",
        arguments = listOf(ValueKind.Text, ValueKind.Whole, ValueKind.Flag, ValueKind.Bytes),
        required = false,
    ),
    Parameter.Callback(name = "onMeasure", arguments = listOf(ValueKind.Decimal), required = true),
    Parameter.Callback(
        name = "onChoose",
        arguments = listOf(styleChoice, ValueKind.ListOf(element = ValueKind.Text)),
        required = true,
    ),
)

private fun slotParameters(): List<Parameter> = listOf(
    Parameter.Slot(name = "any", required = false),
    Parameter.Slot(
        name = "consumer",
        accepts = SlotAccepts.Consumer,
        arguments = listOf(ValueKind.ListOf(element = styleChoice)),
        required = true,
    ),
    Parameter.Slot(
        name = "producer",
        accepts = SlotAccepts.Producer,
        arguments = listOf(ValueKind.Text, ValueKind.Decimal),
        required = true,
    ),
)

private fun valueParameters(): List<Parameter> = listOf(
    Parameter.Value(name = "text", kind = ValueKind.Text, required = true),
    Parameter.Value(name = "whole", kind = ValueKind.Whole, required = true),
    Parameter.Value(name = "decimal", kind = ValueKind.Decimal, required = false, nullable = true),
    Parameter.Value(name = "flag", kind = ValueKind.Flag, required = true),
    Parameter.Value(name = "choice", kind = styleChoice, required = true),
    Parameter.Value(name = "tags", kind = ValueKind.ListOf(element = ValueKind.Text), required = true),
    Parameter.Value(name = "styles", kind = ValueKind.ListOf(element = styleChoice), required = false),
    Parameter.Value(
        name = "counts",
        kind = ValueKind.ListOf(element = ValueKind.Whole),
        required = false,
        nullable = true,
    ),
)
