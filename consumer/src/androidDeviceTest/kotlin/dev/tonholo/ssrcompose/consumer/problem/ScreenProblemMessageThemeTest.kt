package dev.tonholo.ssrcompose.consumer.problem

import android.content.res.Configuration
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.tonholo.ssrcompose.consumer.ScreenProblem
import dev.tonholo.ssrcompose.consumer.testing.InSystemTheme
import dev.tonholo.ssrcompose.consumer.testing.REASON
import dev.tonholo.ssrcompose.consumer.testing.RETRY_LABEL
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

private const val HALF_OF_THE_LUMINANCE_RANGE = 0.5f

private val TEXT_ONLY_INSET_OF_THE_REASON = 0.dp
private val INSET_PAST_THE_RETRY_BORDER = 6.dp

class ScreenProblemMessageThemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun given_a_dark_system_theme__when_the_message_shows__then_its_text_is_readable() {
        // Arrange
        showInSystemTheme(nightMode = Configuration.UI_MODE_NIGHT_YES)

        // Act
        val reasonContrast = luminanceRangeInside(text = REASON, inset = TEXT_ONLY_INSET_OF_THE_REASON)
        val labelContrast = luminanceRangeInside(text = RETRY_LABEL, inset = INSET_PAST_THE_RETRY_BORDER)

        // Assert
        assertTrue(actual = reasonContrast >= HALF_OF_THE_LUMINANCE_RANGE)
        assertTrue(actual = labelContrast >= HALF_OF_THE_LUMINANCE_RANGE)
    }

    @Test
    fun given_a_light_system_theme__when_the_message_shows__then_its_text_is_readable() {
        // Arrange
        showInSystemTheme(nightMode = Configuration.UI_MODE_NIGHT_NO)

        // Act
        val reasonContrast = luminanceRangeInside(text = REASON, inset = TEXT_ONLY_INSET_OF_THE_REASON)
        val labelContrast = luminanceRangeInside(text = RETRY_LABEL, inset = INSET_PAST_THE_RETRY_BORDER)

        // Assert
        assertTrue(actual = reasonContrast >= HALF_OF_THE_LUMINANCE_RANGE)
        assertTrue(actual = labelContrast >= HALF_OF_THE_LUMINANCE_RANGE)
    }

    private fun showInSystemTheme(nightMode: Int) {
        composeRule.setContent {
            InSystemTheme(nightMode = nightMode) {
                ScreenProblemMessage(problem = ScreenProblem.Unavailable(reason = REASON), retry = {})
            }
        }
    }

    private fun luminanceRangeInside(text: String, inset: Dp): Float {
        val pixels = composeRule.onNodeWithText(text = text).captureToImage().toPixelMap()
        val insetPx = with(composeRule.density) { inset.roundToPx() }
        val luminances = (insetPx until pixels.width - insetPx).flatMap { x ->
            (insetPx until pixels.height - insetPx).map { y -> pixels[x, y].luminance() }
        }
        return luminances.max() - luminances.min()
    }
}
