package dev.tonholo.ssrcompose.consumer.problem

import android.content.res.Configuration
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tonholo.ssrcompose.consumer.ScreenProblem
import dev.tonholo.ssrcompose.consumer.testing.GIVEN_HEIGHT
import dev.tonholo.ssrcompose.consumer.testing.GIVEN_WIDTH
import dev.tonholo.ssrcompose.consumer.testing.InSystemTheme
import dev.tonholo.ssrcompose.consumer.testing.REASON
import dev.tonholo.ssrcompose.consumer.testing.RETRY_LABEL
import dev.tonholo.ssrcompose.consumer.testing.ROUNDING_TOLERANCE_IN_DP
import dev.tonholo.ssrcompose.consumer.testing.ROUNDING_TOLERANCE_IN_PX
import dev.tonholo.ssrcompose.consumer.testing.assertDpEquals
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

private const val WIDEST_LINE_GLYPH = "i"
private const val UNBREAKABLE_REASON_LENGTH = 400
private const val EDGES_ON_BOTH_SIDES = 2

private val CONTRACT_CONTENT_PADDING = 24.dp
private val CONTRACT_SPACING = 16.dp
private val CONTRACT_MAX_CONTENT_WIDTH = 480.dp
private val CONTRACT_RETRY_CONTENT_PADDING = 12.dp
private val CONTRACT_RETRY_BORDER_WIDTH = 1.dp
private val CONTRACT_FONT_SIZE = 16.sp
private val WIDER_THAN_THE_MAXIMUM = 800.dp

private val LIGHT_CONTAINER = Color.White
private val LIGHT_CONTENT = Color.Black
private val DARK_CONTAINER = Color.Black
private val DARK_CONTENT = Color.White

class ScreenProblemMessageDefaultLookTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var measurer: TextMeasurer
    private lateinit var density: Density

    @Test
    fun given_the_defaults__when_the_message_shows__then_padding_gap_and_retry_button_have_the_contract_measures() {
        // Arrange
        showWithDefaultsInSpaceWiderThanTheMaximum()

        // Act
        val root = composeRule.onRoot().getBoundsInRoot()
        val reason = composeRule.onNodeWithText(text = UNBREAKABLE_REASON).getBoundsInRoot()
        val retry = composeRule.onNodeWithText(text = RETRY_LABEL).getBoundsInRoot()
        val labelSize = sizeOf(text = RETRY_LABEL)

        // Assert
        assertDpEquals(expected = CONTRACT_CONTENT_PADDING, actual = reason.top - root.top)
        assertDpEquals(expected = CONTRACT_CONTENT_PADDING, actual = root.bottom - retry.bottom)
        assertDpEquals(expected = CONTRACT_SPACING, actual = retry.top - reason.bottom)
        val padding = CONTRACT_RETRY_CONTENT_PADDING * EDGES_ON_BOTH_SIDES
        assertDpEquals(expected = labelSize.width + padding, actual = retry.right - retry.left)
        assertDpEquals(expected = labelSize.height + padding, actual = retry.bottom - retry.top)
    }

    @Test
    fun given_the_defaults__when_a_long_reason_shows__then_the_message_fills_the_maximum_width() {
        // Arrange
        showWithDefaultsInSpaceWiderThanTheMaximum()
        val oneGlyph = sizeOf(text = WIDEST_LINE_GLYPH).width

        // Act
        val reason = composeRule.onNodeWithText(text = UNBREAKABLE_REASON).getBoundsInRoot()
        val widthWithPadding = reason.right - reason.left + CONTRACT_CONTENT_PADDING * EDGES_ON_BOTH_SIDES

        // Assert
        val widest = CONTRACT_MAX_CONTENT_WIDTH.value + ROUNDING_TOLERANCE_IN_DP
        val narrowest = CONTRACT_MAX_CONTENT_WIDTH.value - oneGlyph.value - ROUNDING_TOLERANCE_IN_DP
        assertTrue(actual = widthWithPadding.value in narrowest..widest)
    }

    @Test
    fun given_the_defaults__when_the_message_shows__then_the_retry_border_has_the_contract_width() {
        // Arrange
        showWithDefaultsInSpaceWiderThanTheMaximum()
        val expectedPx = with(density) { CONTRACT_RETRY_BORDER_WIDTH.roundToPx() }

        // Act
        val retry = composeRule.onNodeWithText(text = RETRY_LABEL).captureToImage().toPixelMap()
        val borderColor = retry[retry.width / 2, 0]
        val thickness = (0 until retry.height).takeWhile { y -> retry[retry.width / 2, y] == borderColor }.size

        // Assert
        assertTrue(actual = abs(thickness - expectedPx) <= ROUNDING_TOLERANCE_IN_PX)
    }

    @Test
    fun given_a_light_system_theme__when_the_message_shows_with_defaults__then_it_is_black_on_white() {
        // Arrange
        showWithDefaultsInSystemTheme(nightMode = Configuration.UI_MODE_NIGHT_NO)

        // Act
        val colors = containerAndBorderColors()

        // Assert
        assertEquals(expected = LIGHT_CONTAINER to LIGHT_CONTENT, actual = colors)
    }

    @Test
    fun given_a_dark_system_theme__when_the_message_shows_with_defaults__then_it_is_white_on_black() {
        // Arrange
        showWithDefaultsInSystemTheme(nightMode = Configuration.UI_MODE_NIGHT_YES)

        // Act
        val colors = containerAndBorderColors()

        // Assert
        assertEquals(expected = DARK_CONTAINER to DARK_CONTENT, actual = colors)
    }

    private fun showWithDefaultsInSpaceWiderThanTheMaximum() {
        composeRule.setContent {
            measurer = rememberTextMeasurer()
            density = LocalDensity.current
            ScreenProblemMessage(
                problem = ScreenProblem.Unavailable(reason = UNBREAKABLE_REASON),
                retry = {},
                modifier = Modifier.requiredWidth(width = WIDER_THAN_THE_MAXIMUM),
            )
        }
    }

    private fun showWithDefaultsInSystemTheme(nightMode: Int) {
        composeRule.setContent {
            InSystemTheme(nightMode = nightMode) {
                ScreenProblemMessage(
                    problem = ScreenProblem.Unavailable(reason = REASON),
                    retry = {},
                    modifier = Modifier.size(width = GIVEN_WIDTH, height = GIVEN_HEIGHT),
                )
            }
        }
    }

    private fun containerAndBorderColors(): Pair<Color, Color> {
        val container = composeRule.onRoot().captureToImage().toPixelMap()[0, 0]
        val retry = composeRule.onNodeWithText(text = RETRY_LABEL).captureToImage().toPixelMap()
        return container to retry[retry.width / 2, 0]
    }

    private fun sizeOf(text: String): DpSize = with(density) {
        val size = measurer.measure(text = text, style = LABEL_STYLE).size
        DpSize(width = size.width.toDp(), height = size.height.toDp())
    }

    private companion object {
        val UNBREAKABLE_REASON = WIDEST_LINE_GLYPH.repeat(n = UNBREAKABLE_REASON_LENGTH)
        val LABEL_STYLE = TextStyle(fontSize = CONTRACT_FONT_SIZE)
    }
}
