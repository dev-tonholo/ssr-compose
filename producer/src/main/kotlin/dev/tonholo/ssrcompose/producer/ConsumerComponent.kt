package dev.tonholo.ssrcompose.producer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.key
import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
import dev.tonholo.ssrcompose.core.data.DataValue
import dev.tonholo.ssrcompose.producer.internal.PlacedNode
import dev.tonholo.ssrcompose.producer.internal.ScreenApplier

/**
 * Places one component of a catalog. A bridge function calls this with its own catalog version.
 *
 * Each map keeps its parameters in the order given. A `null` entry is a parameter that was not given, and nothing is
 * placed for it. A required nullable value given `null` is passed as [DataValue.None] and is placed.
 *
 * @param catalogVersion the version of the catalog whose bridge places the component.
 * @param name the component's name in that catalog.
 * @param values the value parameters by name.
 * @param actions the callback parameters by name.
 * @param slots the slot parameters by name.
 */
@InternalSsrComposeApi
@Composable
public fun ConsumerComponent(
    catalogVersion: CatalogVersion,
    name: String,
    values: Map<String, DataValue?> = emptyMap(),
    actions: Map<String, RemoteAction1<String>?> = emptyMap(),
    slots: Map<String, (@Composable () -> Unit)?> = emptyMap(),
) {
    val givenValues = values.withoutOmitted()
    val givenActions = actions.withoutOmitted()
    ComposeNode<PlacedNode.Component, ScreenApplier>(
        factory = { PlacedNode.Component(name = name, catalogVersion = catalogVersion) },
        update = {
            set(givenValues) { this.values = it }
            set(givenActions) { this.actions = it }
        },
        content = {
            slots.withoutOmitted().forEach { (slotName, content) ->
                key(slotName) { PlacedSlot(name = slotName, content = content) }
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

private fun <V : Any> Map<String, V?>.withoutOmitted(): Map<String, V> = buildMap {
    this@withoutOmitted.forEach { (key, value) -> if (value != null) put(key, value) }
}
