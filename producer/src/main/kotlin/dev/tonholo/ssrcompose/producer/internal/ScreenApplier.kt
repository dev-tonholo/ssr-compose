package dev.tonholo.ssrcompose.producer.internal

import androidx.compose.runtime.AbstractApplier

/**
 * Builds the placed tree of one screen.
 *
 * @param onChangesApplied called after each batch of changes lands in the tree.
 */
internal class ScreenApplier(
    root: PlacedNode.Root,
    private val onChangesApplied: () -> Unit = {},
) : AbstractApplier<PlacedNode>(root) {
    override fun insertTopDown(index: Int, instance: PlacedNode) = Unit

    override fun insertBottomUp(index: Int, instance: PlacedNode) {
        current.children.add(index, instance)
    }

    override fun remove(index: Int, count: Int) {
        current.children.subList(index, index + count).clear()
    }

    override fun move(from: Int, to: Int, count: Int) {
        val moved = current.children.subList(from, from + count).toList()
        current.children.subList(from, from + count).clear()
        current.children.addAll(if (to > from) to - count else to, moved)
    }

    override fun onEndChanges() {
        onChangesApplied()
    }

    override fun onClear() {
        root.children.clear()
    }
}
