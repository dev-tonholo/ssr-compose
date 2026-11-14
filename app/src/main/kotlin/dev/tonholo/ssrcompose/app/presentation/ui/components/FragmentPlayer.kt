@file:Suppress("RestrictedApi")

package dev.tonholo.ssrcompose.app.presentation.ui.components

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.player.compose.RemoteDocumentPlayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import java.io.ByteArrayInputStream

@Composable
fun FragmentPlayer(document: ByteArray, modifier: Modifier = Modifier) {
    val coreDocument = remember(document) { document.toCoreDocument() }
    RemoteDocumentPlayer(
        document = coreDocument,
        documentWidth = coreDocument.width,
        documentHeight = coreDocument.height,
        modifier = modifier,
    )
}

private fun ByteArray.toCoreDocument(): CoreDocument =
    CoreDocument().apply {
        initFromBuffer(RemoteComposeBuffer.fromInputStream(ByteArrayInputStream(this@toCoreDocument)))
    }
