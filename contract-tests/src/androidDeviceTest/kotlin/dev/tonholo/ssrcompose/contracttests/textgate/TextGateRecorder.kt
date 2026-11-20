package dev.tonholo.ssrcompose.contracttests.textgate

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Draws each case with foundation [BasicText] at the fixed width and reads back what the device measured.
 * It measures only. Nothing it draws changes what the producer decided.
 */
class TextGateRecorder(
    private val composeRule: ComposeContentTestRule,
    private val fonts: Map<TextGateFont, LoadedFont>,
    private val density: Float,
) {
    fun record(cases: List<TextGateCase>): List<TextGateMeasurement> {
        val layouts = mutableMapOf<String, TextLayoutResult>()
        var current by mutableStateOf(cases.first())
        composeRule.setContent {
            DrawnCase(
                case = current,
                font = fonts.getValue(current.font),
                onLayout = { layouts[current.id] = it },
            )
        }
        return cases.map { case ->
            current = case
            composeRule.waitForIdle()
            measurementOf(case = case, layout = layouts.getValue(case.id))
        }
    }

    private fun measurementOf(case: TextGateCase, layout: TextLayoutResult): TextGateMeasurement {
        val fontSizeInPx = with(densityOf(case)) { case.sizeInSp.sp.toPx() }
        return TextGateMeasurement.of(
            case = case,
            fontSha256 = fonts.getValue(case.font).sha256,
            density = density,
            layout = DeviceTextLayout(
                lineCount = layout.lineCount,
                widthInPx = layout.layoutInput.constraints.maxWidth,
                heightInPx = layout.size.height,
                fontSizeInPx = fontSizeInPx,
            ),
        )
    }

    private fun densityOf(case: TextGateCase) = Density(density = density, fontScale = case.fontScale)

    @Composable
    private fun DrawnCase(case: TextGateCase, font: LoadedFont, onLayout: (TextLayoutResult) -> Unit) {
        CompositionLocalProvider(LocalDensity provides densityOf(case)) {
            Box(modifier = Modifier.width(TextGateCases.WIDTH_IN_DP.dp)) {
                BasicText(
                    text = case.sample.text,
                    style = TextStyle(
                        fontFamily = font.family,
                        fontWeight = FontWeight(case.font.weight),
                        fontStyle = if (case.font.isItalic) FontStyle.Italic else FontStyle.Normal,
                        fontSize = case.sizeInSp.sp,
                    ),
                    onTextLayout = onLayout,
                    overflow = TextOverflow.Clip,
                    softWrap = true,
                    maxLines = Int.MAX_VALUE,
                )
            }
        }
    }
}
