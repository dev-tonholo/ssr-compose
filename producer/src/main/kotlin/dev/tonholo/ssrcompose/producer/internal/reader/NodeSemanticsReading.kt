package dev.tonholo.ssrcompose.producer.internal.reader

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import dev.tonholo.ssrcompose.core.document.NodeRole
import dev.tonholo.ssrcompose.core.document.NodeSemantics

private const val DESCRIPTION_SEPARATOR = " "

/** What accessibility services read for the node, or null when it has none of it. */
internal fun SemanticsNode.readSemantics(): NodeSemantics? {
    val semantics = NodeSemantics(
        description = config.getOrNull(SemanticsProperties.ContentDescription)
            ?.joinToString(separator = DESCRIPTION_SEPARATOR),
        heading = SemanticsProperties.Heading in config,
        role = config.getOrNull(SemanticsProperties.Role)?.toNodeRole(),
    )
    return semantics.takeUnless { it == NodeSemantics() }
}

private fun Role.toNodeRole(): NodeRole? = when (this) {
    Role.Button -> NodeRole.Button
    Role.Checkbox -> NodeRole.Checkbox
    Role.Switch -> NodeRole.Switch
    Role.RadioButton -> NodeRole.RadioButton
    Role.Tab -> NodeRole.Tab
    Role.Image -> NodeRole.Image
    Role.DropdownList -> NodeRole.DropdownList
    Role.ValuePicker -> NodeRole.ValuePicker
    Role.Carousel -> NodeRole.Carousel
    else -> null
}
