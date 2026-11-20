package dev.tonholo.ssrcompose.core.document.internal

import dev.tonholo.ssrcompose.core.document.Click
import dev.tonholo.ssrcompose.core.document.DocumentNode

internal fun requireValidTree(root: DocumentNode) {
    val nodes = nodesOf(root = root)
    val ids = HashSet<Int>()
    nodes.forEach { node -> require(ids.add(node.id)) { "The id ${node.id} is repeated in the document" } }
    val switchers = nodes.filterIsInstance<DocumentNode.Switcher>().associateBy { switcher -> switcher.id }
    nodes.mapNotNull { node -> node.click as? Click.Local }.forEach { click ->
        val variants = switchers[click.switcher]?.variants.orEmpty()
        require(click.variant in variants.indices) {
            "A local click names variant ${click.variant} of switcher ${click.switcher}, which the document lacks"
        }
    }
}

private fun nodesOf(root: DocumentNode): List<DocumentNode> {
    val found = mutableListOf<DocumentNode>()
    val pending = ArrayDeque<DocumentNode>()
    pending.addLast(element = root)
    while (pending.isNotEmpty()) {
        val node = pending.removeLast()
        found += node
        pending.addAll(elements = node.childNodes)
    }
    return found
}

private val DocumentNode.click: Click?
    get() = when (this) {
        is DocumentNode.Box -> click
        is DocumentNode.Text -> click
        is DocumentNode.Scroll, is DocumentNode.Switcher, is DocumentNode.Component -> null
    }

private val DocumentNode.childNodes: List<DocumentNode>
    get() = when (this) {
        is DocumentNode.Box -> children
        is DocumentNode.Text -> emptyList()
        is DocumentNode.Scroll -> children
        is DocumentNode.Switcher -> variants.flatten()
        is DocumentNode.Component -> slots.values.flatMap { slot -> slot.nodes }
    }
