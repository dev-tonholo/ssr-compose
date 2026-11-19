package dev.tonholo.ssrcompose.producer.internal.content

import dev.tonholo.ssrcompose.core.message.PlacedComponent
import dev.tonholo.ssrcompose.core.message.ScreenContent
import dev.tonholo.ssrcompose.producer.internal.PlacedNode

internal fun PlacedNode.Root.toScreenContent(): ScreenContent =
    ScreenContent(components = children.map { it.toPlacedComponent() })

private fun PlacedNode.toPlacedComponent(): PlacedComponent = when (this) {
    is PlacedNode.Component -> PlacedComponent(
        name = name,
        values = values,
        slots = children.associate { it.toPlacedSlot() },
    )

    is PlacedNode.Slot -> error("The slot $name was placed outside a component")
    is PlacedNode.Root -> error("A screen root was placed inside a screen")
}

private fun PlacedNode.toPlacedSlot(): Pair<String, List<PlacedComponent>> = when (this) {
    is PlacedNode.Slot -> name to children.map { it.toPlacedComponent() }
    is PlacedNode.Component -> error("The component $name was placed outside a slot of its parent")
    is PlacedNode.Root -> error("A screen root was placed inside a component")
}
