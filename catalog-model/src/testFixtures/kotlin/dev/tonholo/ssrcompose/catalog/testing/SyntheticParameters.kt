package dev.tonholo.ssrcompose.catalog.testing

import dev.tonholo.ssrcompose.catalog.model.ChoiceList
import dev.tonholo.ssrcompose.catalog.model.Parameter
import dev.tonholo.ssrcompose.core.data.ValueKind
import kotlin.random.Random

private const val PERCENT = 100
private const val TWO_PARAMETERS_UNTIL = 32
private const val THREE_PARAMETERS_UNTIL = 64
private const val FOUR_PARAMETERS_UNTIL = 90
private const val MEDIUM_PARAMETERS_UNTIL = 96
private const val TWO_PARAMETERS = 2
private const val THREE_PARAMETERS = 3
private const val FOUR_PARAMETERS = 4
private val MEDIUM_PARAMETERS = 5..9
private val LARGE_PARAMETERS = 15..30

private const val REQUIRED_PERCENT = 70
private const val TEXT_UNTIL = 35
private const val WHOLE_UNTIL = 45
private const val DECIMAL_UNTIL = 52
private const val FLAG_UNTIL = 62
private const val CHOICE_UNTIL = 72
private const val LIST_UNTIL = 78
private const val CALLBACK_UNTIL = 90
private const val BRIDGE_TEXT_UNTIL = 50
private const val BRIDGE_CALLBACK_UNTIL = 75
private const val MAX_CALLBACK_ARGUMENTS = 2

private const val CHOICE_LISTS = 20
private const val CHOICE_ENTRIES = 5

private val VALUE_NAMES = listOf("title", "subtitle", "label", "caption", "amount", "count", "isEnabled", "style")
private val CALLBACK_NAMES = listOf("onClick", "onDismiss", "onChange", "onSubmit")
private val SLOT_NAMES = listOf("content", "leading", "trailing", "footer")

internal fun syntheticChoiceLists(): List<ChoiceList> =
    List(size = CHOICE_LISTS) { index ->
        ChoiceList(
            name = "SyntheticChoice$index",
            entries = List(size = CHOICE_ENTRIES) { entry -> "Entry$entry" },
        )
    }

internal fun Random.nextParameterCount(): Int {
    val roll = nextInt(until = PERCENT)
    return when {
        roll < TWO_PARAMETERS_UNTIL -> TWO_PARAMETERS
        roll < THREE_PARAMETERS_UNTIL -> THREE_PARAMETERS
        roll < FOUR_PARAMETERS_UNTIL -> FOUR_PARAMETERS
        roll < MEDIUM_PARAMETERS_UNTIL -> nextInt(from = MEDIUM_PARAMETERS.first, until = MEDIUM_PARAMETERS.last + 1)
        else -> nextInt(from = LARGE_PARAMETERS.first, until = LARGE_PARAMETERS.last + 1)
    }
}

internal fun Random.nextParameter(index: Int, palette: ParameterPalette, choices: List<ChoiceList>): Parameter =
    when (palette) {
        ParameterPalette.Full -> nextFullParameter(index = index, choices = choices)
        ParameterPalette.BridgeExperiment -> nextBridgeParameter(index = index)
    }

private fun Random.nextFullParameter(index: Int, choices: List<ChoiceList>): Parameter {
    val roll = nextInt(until = PERCENT)
    return when {
        roll < TEXT_UNTIL -> nextValue(index = index, kind = ValueKind.Text)
        roll < WHOLE_UNTIL -> nextValue(index = index, kind = ValueKind.Whole)
        roll < DECIMAL_UNTIL -> nextValue(index = index, kind = ValueKind.Decimal)
        roll < FLAG_UNTIL -> nextValue(index = index, kind = ValueKind.Flag)
        roll < CHOICE_UNTIL -> nextValue(index = index, kind = ValueKind.Choice(listName = choices.random(this).name))
        roll < LIST_UNTIL -> nextValue(index = index, kind = ValueKind.ListOf(element = ValueKind.Text))
        roll < CALLBACK_UNTIL -> Parameter.Callback(
            name = callbackName(index = index),
            arguments = List(size = nextInt(until = MAX_CALLBACK_ARGUMENTS + 1)) { ValueKind.Text },
            required = nextRequired(),
        )

        else -> Parameter.Slot(name = slotName(index = index), required = nextRequired())
    }
}

private fun Random.nextBridgeParameter(index: Int): Parameter {
    val roll = nextInt(until = PERCENT)
    return when {
        roll < BRIDGE_TEXT_UNTIL -> nextValue(index = index, kind = ValueKind.Text)
        roll < BRIDGE_CALLBACK_UNTIL -> Parameter.Callback(
            name = callbackName(index = index),
            arguments = listOf(ValueKind.Text),
            required = true,
        )

        else -> Parameter.Slot(name = slotName(index = index), required = nextRequired())
    }
}

private fun Random.nextValue(index: Int, kind: ValueKind): Parameter.Value {
    val required = nextRequired()
    return Parameter.Value(
        name = "${VALUE_NAMES[index % VALUE_NAMES.size]}$index",
        kind = kind,
        required = required,
        nullable = !required,
    )
}

private fun Random.nextRequired(): Boolean = nextInt(until = PERCENT) < REQUIRED_PERCENT

private fun callbackName(index: Int): String = "${CALLBACK_NAMES[index % CALLBACK_NAMES.size]}$index"

private fun slotName(index: Int): String = "${SLOT_NAMES[index % SLOT_NAMES.size]}$index"
