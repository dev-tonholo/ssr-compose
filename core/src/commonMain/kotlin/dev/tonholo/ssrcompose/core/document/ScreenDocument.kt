package dev.tonholo.ssrcompose.core.document

import dev.tonholo.ssrcompose.core.document.internal.RootBoxSerializer
import dev.tonholo.ssrcompose.core.document.internal.requireValidTree
import kotlinx.serialization.Serializable

/**
 * A screen the producer composed, measured and placed. [root] is the box the producer wraps around every screen,
 * with the size of the viewport.
 *
 * @throws IllegalArgumentException when an id is repeated, or a local click names a switcher or a variant the
 * document does not have.
 */
@Serializable
public data class ScreenDocument(
    @Serializable(with = RootBoxSerializer::class)
    val root: DocumentNode.Box,
) {
    init {
        requireValidTree(root = root)
    }
}
