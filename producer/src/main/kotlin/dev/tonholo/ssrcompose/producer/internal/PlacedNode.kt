package dev.tonholo.ssrcompose.producer.internal

import dev.tonholo.ssrcompose.producer.RemoteAction1

internal sealed class PlacedNode {
    val children: MutableList<PlacedNode> = mutableListOf()

    class Root : PlacedNode()

    class Component(
        val name: String,
        val catalogVersion: String,
    ) : PlacedNode() {
        var values: Map<String, String> = emptyMap()
        var actions: Map<String, RemoteAction1<String>> = emptyMap()
    }

    class Slot(val name: String) : PlacedNode()
}
