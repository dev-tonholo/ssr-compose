@file:Suppress("RestrictedApi")

package dev.tonholo.ssrcompose.app.presentation.ui.components

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import dev.tonholo.ssrcompose.protocol.ActionId
import dev.tonholo.ssrcompose.protocol.FragmentNode
import java.io.ByteArrayInputStream

const val FRAGMENT_PLAYER_TAG = "fragment-player"
private const val FRAGMENT_ERROR_TEXT = "Could not play fragment"

@Composable
fun FragmentPlayer(fragment: FragmentNode, onAction: (ActionId) -> Unit, modifier: Modifier = Modifier) {
    FragmentPlayer(fragment = fragment, onAction = onAction, modifier = modifier, parse = ::parseFragment)
}

@Composable
internal fun FragmentPlayer(
    fragment: FragmentNode,
    onAction: (ActionId) -> Unit,
    modifier: Modifier,
    parse: (ByteArray) -> PlayableFragment,
) {
    val playable = remember(fragment) { runCatching { parse(fragment.document) } }.getOrNull()
    if (playable == null) {
        Text(text = FRAGMENT_ERROR_TEXT, modifier = modifier)
        return
    }
    val latestOnAction by rememberUpdatedState(newValue = onAction)
    RemoteDocumentPlayer(
        document = playable.document,
        documentWidth = playable.width,
        documentHeight = playable.height,
        modifier = modifier
            .testTag(tag = FRAGMENT_PLAYER_TAG)
            .fillMaxWidth()
            .aspectRatio(ratio = playable.width.toFloat() / playable.height),
        onNamedAction = { name, _, _ -> latestOnAction(ActionId(value = name)) },
    )
}

internal class PlayableFragment(val document: CoreDocument, val width: Int, val height: Int)

internal fun parseFragment(bytes: ByteArray): PlayableFragment {
    require(bytes.isNotEmpty()) { "Fragment has no bytes" }
    val document = CoreDocument()
    document.initFromBuffer(RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(bytes)))
    val header = document.operations.filterIsInstance<Header>().single()
    val width = header.intTag(tag = Header.DOC_WIDTH)
    val height = header.intTag(tag = Header.DOC_HEIGHT)
    require(width > 0 && height > 0) { "Fragment has no size" }
    return PlayableFragment(document = document, width = width, height = height)
}

private fun Header.intTag(tag: Short): Int = get(tag) as? Int ?: 0
