package dev.tonholo.ssrcompose.producer.testing

import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.SemanticsModifierNode
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription

/**
 * A content description that only a modifier node's semantics reads. Unlike `Modifier.semantics`, inspecting this
 * modifier does not run [description], so reading the screen never reads what it reads.
 */
internal fun Modifier.describedBySemanticsNode(description: () -> String): Modifier =
    this then DescriptionElement(description = description)

private data class DescriptionElement(val description: () -> String) : ModifierNodeElement<DescriptionNode>() {
    override fun create(): DescriptionNode = DescriptionNode(description = description)

    override fun update(node: DescriptionNode) {
        node.description = description
    }
}

private class DescriptionNode(var description: () -> String) : Modifier.Node(), SemanticsModifierNode {
    override fun SemanticsPropertyReceiver.applySemantics() {
        contentDescription = description()
    }
}
