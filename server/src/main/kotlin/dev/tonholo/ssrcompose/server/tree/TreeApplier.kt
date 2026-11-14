package dev.tonholo.ssrcompose.server.tree

import androidx.compose.runtime.AbstractApplier

internal class TreeApplier(root: MutableNode.Column) : AbstractApplier<MutableNode>(root) {
    override fun insertTopDown(index: Int, instance: MutableNode) {
        current.asColumn().children.add(index, instance)
    }

    override fun insertBottomUp(index: Int, instance: MutableNode) = Unit

    override fun remove(index: Int, count: Int) {
        val children = current.asColumn().children
        repeat(count) { children.removeAt(index) }
    }

    override fun move(from: Int, to: Int, count: Int) {
        val children = current.asColumn().children
        val moved = List(count) { children.removeAt(from) }
        val target = if (to > from) to - count else to
        children.addAll(target, moved)
    }

    override fun onClear() {
        (root as MutableNode.Column).children.clear()
    }

    private fun MutableNode.asColumn(): MutableNode.Column =
        checkNotNull(this as? MutableNode.Column) { "Only a column can hold children" }
}
