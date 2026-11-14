@file:Suppress("RestrictedApi")

package dev.tonholo.ssrcompose.server.screen

import androidx.compose.remote.creation.dsl.Modifier
import androidx.compose.remote.creation.dsl.RcActionScope
import androidx.compose.remote.creation.dsl.RcScope
import androidx.compose.remote.creation.dsl.RcSp
import androidx.compose.remote.creation.dsl.background
import androidx.compose.remote.creation.dsl.clip
import androidx.compose.remote.creation.dsl.fillMaxHeight
import androidx.compose.remote.creation.dsl.fillMaxSize
import androidx.compose.remote.creation.dsl.horizontalWeight
import androidx.compose.remote.creation.dsl.onClick
import androidx.compose.remote.creation.dsl.padding
import androidx.compose.remote.creation.modifiers.RoundedRectShape
import androidx.compose.runtime.Composable
import dev.tonholo.ssrcompose.protocol.ActionId
import dev.tonholo.ssrcompose.server.tree.RemoteFragment

const val COUNTER_BANNER_WIDTH_PX = 600
const val COUNTER_BANNER_HEIGHT_PX = 200
const val LOCAL_TOGGLE_OFF_TEXT = "Local: off"
const val LOCAL_TOGGLE_ON_TEXT = "Local: on"
private const val TOGGLE_OFF_STATE = 0
private const val TOGGLE_ON_STATE = 1
private const val COUNT_BACKGROUND = 0xFF00695C
private const val TOGGLE_OFF_BACKGROUND = 0xFF455A64
private const val TOGGLE_ON_BACKGROUND = 0xFFEF6C00
private const val TEXT_COLOR = 0xFFFFFFFF
private const val BOX_PADDING = 24f
private const val TEXT_SIZE = 36f
private const val CORNER_RADIUS = 32f
private const val COUNT_WEIGHT = 2f
private const val TOGGLE_WEIGHT = 1f

private val cornerShape = RoundedRectShape(CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS, CORNER_RADIUS)

fun bannerText(count: Int): String = "Tapped $count times. Tap me."

@Composable
fun CounterBanner(count: Int, onTap: ActionId) {
    RemoteFragment(COUNTER_BANNER_WIDTH_PX, COUNTER_BANNER_HEIGHT_PX, count, onTap) {
        val toggleState = remoteInteger(TOGGLE_OFF_STATE)
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .horizontalWeight(COUNT_WEIGHT)
                    .fillMaxHeight()
                    .clip(cornerShape)
                    .background(COUNT_BACKGROUND.toInt())
                    .padding(BOX_PADDING)
                    .onClick { hostAction(onTap.value) },
            ) {
                Text(text = bannerText(count), color = TEXT_COLOR, fontSize = RcSp(TEXT_SIZE))
            }
            Box(modifier = Modifier.horizontalWeight(TOGGLE_WEIGHT).fillMaxHeight()) {
                StateLayout(stateIndex = toggleState, modifier = Modifier.fillMaxSize()) {
                    toggleBox(
                        text = LOCAL_TOGGLE_OFF_TEXT,
                        backgroundColor = TOGGLE_OFF_BACKGROUND,
                        onClick = { setValue(toggleState, TOGGLE_ON_STATE) },
                    )
                    toggleBox(
                        text = LOCAL_TOGGLE_ON_TEXT,
                        backgroundColor = TOGGLE_ON_BACKGROUND,
                        onClick = { setValue(toggleState, TOGGLE_OFF_STATE) },
                    )
                }
            }
        }
    }
}

private fun RcScope.toggleBox(text: String, backgroundColor: Long, onClick: RcActionScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(cornerShape)
            .background(backgroundColor.toInt())
            .padding(BOX_PADDING)
            .onClick(onClick),
    ) {
        Text(text = text, color = TEXT_COLOR, fontSize = RcSp(TEXT_SIZE))
    }
}
