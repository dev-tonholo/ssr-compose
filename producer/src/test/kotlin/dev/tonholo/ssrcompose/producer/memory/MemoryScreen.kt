package dev.tonholo.ssrcompose.producer.memory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.tonholo.ssrcompose.producer.ScaledBasicText
import dev.tonholo.ssrcompose.producer.internal.scene.SceneSetup
import dev.tonholo.ssrcompose.producer.testing.robotoStyle

private const val SCREEN_ROWS = 50
private const val TEXT_SIZE_SP = 14
private const val ROW_PADDING_DP = 4
private const val GAP_DP = 8
private const val ROW_COLOR = 0xFFEEEEEE
private const val WIDTH_PX = 1080
private const val HEIGHT_PX = 2340
private const val DENSITY = 3f
private const val TEXT_WIDTH_DP = 100
private const val TEXT_HEIGHT_DP = 16

internal val memoryScreenSetup = SceneSetup(
    widthPx = WIDTH_PX,
    heightPx = HEIGHT_PX,
    density = Density(density = DENSITY, fontScale = 1f),
    layoutDirection = LayoutDirection.Ltr,
)

/** The 50-row screen of the cost runs: 100 texts in the bundled Roboto, and every text different per [seed]. */
@Composable
internal fun MemoryScreen(seed: Int, modifier: Modifier = Modifier) {
    val style = robotoStyle(sizeSp = TEXT_SIZE_SP)
    FiftyRows(modifier = modifier) { row ->
        ScaledBasicText(text = "Row $row of screen $seed", style = style)
        Spacer(modifier = Modifier.width(GAP_DP.dp))
        ScaledBasicText(text = "Value ${row * (seed + 1)}", style = style)
    }
}

/** [MemoryScreen] with each of its texts replaced by an empty box, to tell what its texts cost. */
@Composable
internal fun MemoryScreenWithoutTexts() {
    FiftyRows {
        Box(modifier = Modifier.size(width = TEXT_WIDTH_DP.dp, height = TEXT_HEIGHT_DP.dp))
        Spacer(modifier = Modifier.width(GAP_DP.dp))
        Box(modifier = Modifier.size(width = TEXT_WIDTH_DP.dp, height = TEXT_HEIGHT_DP.dp))
    }
}

@Composable
private fun FiftyRows(modifier: Modifier = Modifier, content: @Composable (row: Int) -> Unit) {
    Column(modifier = modifier.fillMaxSize().background(Color.White)) {
        repeat(SCREEN_ROWS) { row ->
            Row(modifier = Modifier.fillMaxWidth().padding(ROW_PADDING_DP.dp).background(Color(ROW_COLOR))) {
                content(row)
            }
        }
    }
}
