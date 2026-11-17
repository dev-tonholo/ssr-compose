package dev.tonholo.ssrcompose.producer.internal.content

import dev.tonholo.ssrcompose.core.data.DataValue
import dev.tonholo.ssrcompose.core.message.PlacedComponent
import dev.tonholo.ssrcompose.core.message.ScreenContent
import dev.tonholo.ssrcompose.producer.internal.PlacedNode

internal fun PlacedNode.Root.toScreenContent(): ScreenContent =
    ScreenContent(components = children.toPlacedComponents())

private fun PlacedNode.Component.toPlacedComponent(): PlacedComponent =
    PlacedComponent(
        name = name,
        values = values.mapValues { (_, text) -> DataValue.Text(value = text) },
        slots = children.filterIsInstance<PlacedNode.Slot>().associate { slot ->
            slot.name to slot.children.toPlacedComponents()
        },
    )

private fun List<PlacedNode>.toPlacedComponents(): List<PlacedComponent> =
    filterIsInstance<PlacedNode.Component>().map { it.toPlacedComponent() }
