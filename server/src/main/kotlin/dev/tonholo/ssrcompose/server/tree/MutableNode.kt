package dev.tonholo.ssrcompose.server.tree

import dev.tonholo.ssrcompose.protocol.ColumnNode
import dev.tonholo.ssrcompose.protocol.FragmentNode
import dev.tonholo.ssrcompose.protocol.ReferenceNode
import dev.tonholo.ssrcompose.protocol.ScreenNode
import kotlinx.serialization.json.JsonObject

internal sealed interface MutableNode {
    fun toScreenNode(): ScreenNode

    class Column : MutableNode {
        val children: MutableList<MutableNode> = mutableListOf()

        override fun toScreenNode(): ScreenNode =
            ColumnNode(children = children.map(MutableNode::toScreenNode))
    }

    class Reference(
        var componentId: String,
        var props: JsonObject,
    ) : MutableNode {
        override fun toScreenNode(): ScreenNode =
            ReferenceNode(componentId = componentId, props = props)
    }

    class Fragment(var document: ByteArray) : MutableNode {
        override fun toScreenNode(): ScreenNode = FragmentNode(document = document)
    }
}
