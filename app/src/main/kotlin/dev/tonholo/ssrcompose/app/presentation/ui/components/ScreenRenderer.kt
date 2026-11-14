package dev.tonholo.ssrcompose.app.presentation.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.tonholo.ssrcompose.app.presentation.ui.Dimens
import dev.tonholo.ssrcompose.protocol.ColumnNode
import dev.tonholo.ssrcompose.protocol.FragmentNode
import dev.tonholo.ssrcompose.protocol.ReferenceNode
import dev.tonholo.ssrcompose.protocol.ScreenNode

private const val FRAGMENT_ASPECT_RATIO = 600f / 160f

@Composable
fun ScreenRenderer(node: ScreenNode, registry: ComponentRegistry, modifier: Modifier = Modifier) {
    when (node) {
        is ColumnNode -> Column(
            modifier = modifier.padding(all = Dimens.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(space = Dimens.ItemSpacing),
        ) {
            node.children.forEach { child -> ScreenRenderer(node = child, registry = registry) }
        }
        is ReferenceNode -> registry.Render(node = node)
        is FragmentNode -> FragmentPlayer(
            document = node.document,
            modifier = modifier.fillMaxWidth().aspectRatio(ratio = FRAGMENT_ASPECT_RATIO),
        )
    }
}
