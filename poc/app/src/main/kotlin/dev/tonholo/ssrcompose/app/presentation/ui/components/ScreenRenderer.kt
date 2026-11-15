package dev.tonholo.ssrcompose.app.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.tonholo.ssrcompose.app.presentation.ui.Dimens
import dev.tonholo.ssrcompose.protocol.ActionId
import dev.tonholo.ssrcompose.protocol.ColumnNode
import dev.tonholo.ssrcompose.protocol.FragmentNode
import dev.tonholo.ssrcompose.protocol.ReferenceNode
import dev.tonholo.ssrcompose.protocol.ScreenNode

@Composable
fun ScreenRenderer(
    node: ScreenNode,
    registry: ComponentRegistry,
    onAction: (ActionId) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (node) {
        is ColumnNode -> Column(
            modifier = modifier.padding(all = Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(space = Dimens.ItemSpacing),
        ) {
            node.children.forEach { child -> ScreenRenderer(node = child, registry = registry, onAction = onAction) }
        }
        is ReferenceNode -> registry.Render(node = node, onAction = onAction)
        is FragmentNode -> FragmentPlayer(fragment = node, onAction = onAction)
    }
}
