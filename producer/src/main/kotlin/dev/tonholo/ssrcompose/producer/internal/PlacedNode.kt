package dev.tonholo.ssrcompose.producer.internal

import dev.tonholo.ssrcompose.core.catalog.CatalogVersion
import dev.tonholo.ssrcompose.core.data.DataValue
import dev.tonholo.ssrcompose.producer.RemoteAction1

internal sealed class PlacedNode {
    val children: MutableList<PlacedNode> = mutableListOf()

    class Root : PlacedNode()

    class Component(
        val name: String,
        val catalogVersion: CatalogVersion,
    ) : PlacedNode() {
        var values: Map<String, DataValue> = emptyMap()
        var actions: Map<String, RemoteAction1<String>> = emptyMap()
    }

    class Slot(val name: String) : PlacedNode()
}
