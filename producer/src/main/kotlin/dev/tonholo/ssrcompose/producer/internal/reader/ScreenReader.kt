package dev.tonholo.ssrcompose.producer.internal.reader

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.Density
import dev.tonholo.ssrcompose.core.document.DocumentNode
import dev.tonholo.ssrcompose.core.document.ScreenDocument

/**
 * Reads what a scene composed, measured and placed into a positioned document, from the unmerged semantics tree
 * under [root], the producer's root `Box`. A node with nothing to draw, no click and no semantics is left out, and
 * its children are placed in the node it would have been sent in. Clicks are not sent yet.
 */
internal fun readScreen(root: SemanticsNode, density: Density): ScreenRead = try {
    ScreenRead.Document(document = ScreenDocument(root = NodeReader(density = density).readRoot(root = root)))
} catch (refusal: NotSendableException) {
    ScreenRead.NotSendable(reason = refusal.reason)
}

private class NodeReader(private val density: Density) {
    fun readRoot(root: SemanticsNode): DocumentNode.Box {
        val box = root.ownBox()
        return box.toBox(id = root.id, parent = box, children = readChildren(node = root, parent = box))
    }

    private fun readChildren(node: SemanticsNode, parent: NodeBox): List<DocumentNode> =
        node.children.flatMap { child -> read(node = child, parent = parent) }

    private fun read(node: SemanticsNode, parent: NodeBox): List<DocumentNode> {
        val modifiers = node.inspectedModifiers()
        node.refuseUnsupported(modifiers = modifiers)
        modifiers.refuseGraphicsLayers()
        node.refuseTransformedLayout()
        val decorated = modifiers.readDecoration(density = density)
        val scrolls = modifiers.named(SCROLL_MODIFIER)
        node.refuseUnsupportedScrolls(scrolls = scrolls)
        val scroll = scrolls.singleOrNull()
        return when {
            SemanticsProperties.Text in node.config -> listOf(node.readTextIn(parent = parent, decorated = decorated))
            scroll != null -> listOf(node.readScrollIn(parent = parent, scroll = scroll, decorated = decorated))
            decorated != null || node.isSent() -> listOf(node.readBoxIn(parent = parent, decorated = decorated))
            else -> readChildren(node = node, parent = parent)
        }
    }

    private fun SemanticsNode.readTextIn(parent: NodeBox, decorated: DecoratedBox?): DocumentNode.Text {
        if (decorated != null) refuse(reason = NotSendableReason.TextWithDecoration)
        return readText(placement = ownBox().placedIn(parent = parent, density = density.density))
    }

    private fun SemanticsNode.readScrollIn(
        parent: NodeBox,
        scroll: InspectedModifier,
        decorated: DecoratedBox?,
    ): DocumentNode.Scroll {
        if (decorated != null) refuse(reason = NotSendableReason.ScrollWithDecoration)
        val box = scroll.coordinates.toNodeBox()
        return scroll.readScroll(
            id = id,
            placement = box.placedIn(parent = parent, density = density.density),
            density = density.density,
            children = readChildren(node = this, parent = box),
        )
    }

    private fun SemanticsNode.readBoxIn(parent: NodeBox, decorated: DecoratedBox?): DocumentNode.Box {
        val box = decorated?.box ?: ownBox()
        val sent = box.toBox(id = id, parent = parent, children = readChildren(node = this, parent = box))
        val semantics = readSemantics()
        return documentValue { sent.copy(decoration = decorated?.decoration, semantics = semantics) }
    }

    private fun NodeBox.toBox(id: Int, parent: NodeBox, children: List<DocumentNode>): DocumentNode.Box {
        val placement = placedIn(parent = parent, density = density.density)
        return documentValue {
            DocumentNode.Box(
                id = id,
                x = placement.x,
                y = placement.y,
                width = placement.width,
                height = placement.height,
                children = children,
            )
        }
    }
}

private fun SemanticsNode.isSent(): Boolean = SemanticsActions.OnClick in config || readSemantics() != null

private fun SemanticsNode.refuseUnsupportedScrolls(scrolls: List<InspectedModifier>) {
    if (scrolls.size > 1) refuse(reason = NotSendableReason.SeveralScrolls)
    if (scrolls.isNotEmpty() && SemanticsProperties.Text in config) refuse(reason = NotSendableReason.TextWithScroll)
}

private fun SemanticsNode.refuseUnsupported(modifiers: List<InspectedModifier>) {
    if (SemanticsProperties.EditableText in config || SemanticsActions.SetText in config) {
        refuse(reason = NotSendableReason.TextField)
    }
    if (SemanticsProperties.IndexForKey in config) refuse(reason = NotSendableReason.LazyList)
    val image = modifiers.named(PAINT_MODIFIER).isNotEmpty() || config.getOrNull(SemanticsProperties.Role) == Role.Image
    if (image) refuse(reason = NotSendableReason.Image)
}
