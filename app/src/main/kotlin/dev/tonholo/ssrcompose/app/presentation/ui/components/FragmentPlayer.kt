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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import java.io.ByteArrayInputStream

const val FRAGMENT_PLAYER_TAG = "fragment-player"
private const val FRAGMENT_ERROR_TEXT = "Could not play fragment"

@Composable
fun FragmentPlayer(document: ByteArray, modifier: Modifier = Modifier) {
    val fragment: PlayableFragment? = remember(document) { runCatching { document.toFragment() } }.getOrNull()
    if (fragment == null) {
        Text(text = FRAGMENT_ERROR_TEXT, modifier = modifier)
        return
    }
    RemoteDocumentPlayer(
        document = fragment.document,
        documentWidth = fragment.width,
        documentHeight = fragment.height,
        modifier = modifier
            .testTag(tag = FRAGMENT_PLAYER_TAG)
            .fillMaxWidth()
            .aspectRatio(ratio = fragment.width.toFloat() / fragment.height),
    )
}

private class PlayableFragment(val document: CoreDocument, val width: Int, val height: Int)

private fun ByteArray.toFragment(): PlayableFragment {
    require(isNotEmpty()) { "Fragment has no bytes" }
    val document = CoreDocument()
    document.initFromBuffer(RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(this)))
    val header = document.operations.filterIsInstance<Header>().single()
    val width = header.intTag(tag = Header.DOC_WIDTH)
    val height = header.intTag(tag = Header.DOC_HEIGHT)
    require(width > 0 && height > 0) { "Fragment has no size" }
    return PlayableFragment(document = document, width = width, height = height)
}

private fun Header.intTag(tag: Short): Int = get(tag) as? Int ?: 0
