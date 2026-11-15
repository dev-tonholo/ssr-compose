@file:Suppress("RestrictedApi")

package dev.tonholo.ssrcompose.server.tree

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriter.HTag
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.RcScope
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.remember

private val serverProfile = RcProfile(
    profile = Profile(
        CoreDocument.DOCUMENT_API_LEVEL,
        RcProfiles.PROFILE_ANDROIDX,
        RcPlatformServices.None,
    ) { _, profile, _ -> RemoteComposeWriter(profile) },
    experimental = true,
)

private fun buildDocument(widthPx: Int, heightPx: Int, content: RcScope.() -> Unit): ByteArray =
    createRcBuffer(
        serverProfile,
        true,
        HTag(Header.DOC_WIDTH, widthPx),
        HTag(Header.DOC_HEIGHT, heightPx),
        content = content,
    )

@Composable
fun RemoteFragment(widthPx: Int, heightPx: Int, vararg keys: Any?, content: RcScope.() -> Unit) {
    val document = remember(widthPx, heightPx, *keys) { buildDocument(widthPx, heightPx, content) }
    ComposeNode<MutableNode.Fragment, TreeApplier>(
        factory = { MutableNode.Fragment(document = document) },
        update = { set(document) { this.document = it } },
    )
}
