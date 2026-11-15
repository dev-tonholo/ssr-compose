@file:Suppress("RestrictedApi")

package dev.tonholo.ssrcompose.app.presentation.ui.components

import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.RcProfiles
import androidx.compose.remote.core.operations.Header
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemoteComposeWriter.HTag
import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcProfile
import androidx.compose.remote.creation.dsl.createRcBuffer
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.onClick
import androidx.compose.remote.creation.profile.Profile

private val testProfile = RcProfile(
    profile = Profile(
        CoreDocument.DOCUMENT_API_LEVEL,
        RcProfiles.PROFILE_ANDROIDX,
        RcPlatformServices.None,
    ) { _, profile, _ -> RemoteComposeWriter(profile) },
    experimental = true,
)

fun boxDocument(widthPx: Int, heightPx: Int): ByteArray =
    createRcBuffer(
        testProfile,
        true,
        HTag(Header.DOC_WIDTH, widthPx),
        HTag(Header.DOC_HEIGHT, heightPx),
    ) { Box() }

fun clickableBoxDocument(widthPx: Int, heightPx: Int, actionId: String): ByteArray =
    createRcBuffer(
        testProfile,
        true,
        HTag(Header.DOC_WIDTH, widthPx),
        HTag(Header.DOC_HEIGHT, heightPx),
    ) { Box(modifier = Modifier.fillMaxSize().onClick { hostAction(actionId) }) }
