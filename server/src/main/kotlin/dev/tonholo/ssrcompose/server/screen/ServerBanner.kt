@file:Suppress("RestrictedApi")

package dev.tonholo.ssrcompose.server.screen

import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.padding
import androidx.compose.runtime.Composable
import dev.tonholo.ssrcompose.server.tree.RemoteFragment

const val BANNER_WIDTH_PX = 600
const val BANNER_HEIGHT_PX = 160
const val BANNER_TEXT = "Drawn by the server"
private const val BANNER_BACKGROUND = 0xFF3F51B5
private const val BANNER_TEXT_COLOR = 0xFFFFFFFF
private const val BANNER_PADDING = 24f

@Composable
fun ServerBanner() {
    RemoteFragment(widthPx = BANNER_WIDTH_PX, heightPx = BANNER_HEIGHT_PX) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BANNER_BACKGROUND.toInt())
                .padding(BANNER_PADDING),
        ) {
            Text(text = BANNER_TEXT, color = BANNER_TEXT_COLOR)
        }
    }
}
