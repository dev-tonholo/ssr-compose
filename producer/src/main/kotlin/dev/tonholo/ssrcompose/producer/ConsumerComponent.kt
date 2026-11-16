package dev.tonholo.ssrcompose.producer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import dev.tonholo.ssrcompose.producer.internal.PlacedNode
import dev.tonholo.ssrcompose.producer.internal.ScreenApplier

/**
 * Places one component of a catalog. A bridge function calls this with its own catalog version.
 *
 * The names and the values are parallel arrays: the value at an index belongs to the name at the same index. A null
 * value or a null slot is a parameter that was not given, and nothing is placed for it.
 */
@Composable
fun ConsumerComponent(
    catalogVersion: String,
    name: String,
    valueNames: Array<String>,
    values: Array<String?>,
    actionNames: Array<String>,
    actions: Array<RemoteAction1<String>>,
    slotNames: Array<String>,
    slots: Array<(@Composable () -> Unit)?>,
) {
    val givenValues = valueNames.zip(values).mapNotNull { (key, value) -> value?.let { key to it } }.toMap()
    val givenActions = actionNames.zip(actions).toMap()
    ComposeNode<PlacedNode.Component, ScreenApplier>(
        factory = { PlacedNode.Component(name = name, catalogVersion = catalogVersion) },
        update = {
            set(givenValues) { this.values = it }
            set(givenActions) { this.actions = it }
        },
        content = {
            slotNames.zip(slots).forEach { (slotName, content) ->
                if (content != null) PlacedSlot(name = slotName, content = content)
            }
        },
    )
}

@Composable
private fun PlacedSlot(name: String, content: @Composable () -> Unit) {
    ComposeNode<PlacedNode.Slot, ScreenApplier>(
        factory = { PlacedNode.Slot(name = name) },
        update = {},
        content = { content() },
    )
}
