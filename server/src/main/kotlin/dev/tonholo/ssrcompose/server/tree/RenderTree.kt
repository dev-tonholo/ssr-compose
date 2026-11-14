package dev.tonholo.ssrcompose.server.tree

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import dev.tonholo.ssrcompose.protocol.ScreenNode
import kotlin.coroutines.coroutineContext

suspend fun renderTree(content: @Composable () -> Unit): ScreenNode {
    val root = MutableNode.Column()
    val recomposer = Recomposer(coroutineContext)
    val composition = Composition(applier = TreeApplier(root), parent = recomposer)
    try {
        composition.setContent(content)
        return root.toScreenNode()
    } finally {
        composition.dispose()
        recomposer.cancel()
    }
}
