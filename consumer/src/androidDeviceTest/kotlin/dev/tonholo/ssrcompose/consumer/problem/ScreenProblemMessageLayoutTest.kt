package dev.tonholo.ssrcompose.consumer.problem

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tonholo.ssrcompose.consumer.ScreenProblem
import dev.tonholo.ssrcompose.consumer.testing.GIVEN_HEIGHT
import dev.tonholo.ssrcompose.consumer.testing.GIVEN_WIDTH
import dev.tonholo.ssrcompose.consumer.testing.LONG_REASON
import dev.tonholo.ssrcompose.consumer.testing.REASON
import dev.tonholo.ssrcompose.consumer.testing.RETRY_LABEL
import dev.tonholo.ssrcompose.consumer.testing.ROUNDING_TOLERANCE_IN_PX
import dev.tonholo.ssrcompose.consumer.testing.assertDpEquals
import kotlin.math.abs
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

private val GIVEN_START = 40.dp
private val GIVEN_TOP = 32.dp
private val GIVEN_END = 20.dp
private val GIVEN_BOTTOM = 48.dp
private val GIVEN_SPACING = 30.dp
private val GIVEN_MAX_CONTENT_WIDTH = 200.dp
private val GIVEN_SMALL_RETRY_PADDING = 12.dp
private val GIVEN_LARGE_RETRY_PADDING = 20.dp
private val GIVEN_BORDER_WIDTH = 6.dp
private val GIVEN_LARGE_FONT_SIZE = 32.sp

class ScreenProblemMessageLayoutTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val unavailable = ScreenProblem.Unavailable(reason = REASON)

    @Test
    fun given_a_content_padding__when_shown__then_the_message_sits_that_far_from_each_edge() {
        // Arrange
        val padding = PaddingValues(start = GIVEN_START, top = GIVEN_TOP, end = GIVEN_END, bottom = GIVEN_BOTTOM)

        // Act
        composeRule.setContent {
            ScreenProblemMessage(problem = unavailable, retry = {}, contentPadding = padding)
        }
        val root = composeRule.onRoot().getBoundsInRoot()
        val reason = composeRule.onNodeWithText(text = REASON).getBoundsInRoot()
        val retry = composeRule.onNodeWithText(text = RETRY_LABEL).getBoundsInRoot()

        // Assert
        assertDpEquals(expected = GIVEN_START, actual = reason.left - root.left)
        assertDpEquals(expected = GIVEN_TOP, actual = reason.top - root.top)
        assertDpEquals(expected = GIVEN_END, actual = root.right - reason.right)
        assertDpEquals(expected = GIVEN_BOTTOM, actual = root.bottom - retry.bottom)
    }

    @Test
    fun given_a_spacing__when_shown__then_the_message_and_the_button_are_that_far_apart() {
        // Arrange
        val spacing = GIVEN_SPACING

        // Act
        composeRule.setContent {
            ScreenProblemMessage(problem = unavailable, retry = {}, spacing = spacing)
        }
        val reason = composeRule.onNodeWithText(text = REASON).getBoundsInRoot()
        val retry = composeRule.onNodeWithText(text = RETRY_LABEL).getBoundsInRoot()

        // Assert
        assertDpEquals(expected = spacing, actual = retry.top - reason.bottom)
    }

    @Test
    fun given_a_maximum_content_width__when_a_long_reason_shows__then_the_message_is_no_wider() {
        // Arrange
        val problem = ScreenProblem.Unavailable(reason = LONG_REASON)

        // Act
        composeRule.setContent {
            ScreenProblemMessage(
                problem = problem,
                retry = {},
                modifier = Modifier.sized(),
                contentPadding = PaddingValues(start = GIVEN_START, end = GIVEN_END),
                maxContentWidth = GIVEN_MAX_CONTENT_WIDTH,
            )
        }
        val reason = composeRule.onNodeWithText(text = LONG_REASON).getBoundsInRoot()

        // Assert
        val widthWithInsets = reason.right - reason.left + GIVEN_START + GIVEN_END
        assertTrue(actual = widthWithInsets <= GIVEN_MAX_CONTENT_WIDTH)
    }

    @Test
    fun given_a_retry_content_padding__when_shown__then_the_button_grows_by_that_padding() {
        // Arrange
        var padding by mutableStateOf(value = PaddingValues(all = GIVEN_SMALL_RETRY_PADDING))
        composeRule.setContent {
            ScreenProblemMessage(problem = unavailable, retry = {}, retryContentPadding = padding)
        }

        // Act
        val smallHeight = heightOf(text = RETRY_LABEL)
        composeRule.runOnIdle { padding = PaddingValues(all = GIVEN_LARGE_RETRY_PADDING) }
        val largeHeight = heightOf(text = RETRY_LABEL)

        // Assert
        val addedOnEachSide = GIVEN_LARGE_RETRY_PADDING - GIVEN_SMALL_RETRY_PADDING
        assertDpEquals(expected = addedOnEachSide * 2, actual = largeHeight - smallHeight)
    }

    @Test
    fun given_a_retry_border_width__when_shown__then_the_border_is_that_thick() {
        // Arrange
        val expectedPx = with(composeRule.density) { GIVEN_BORDER_WIDTH.roundToPx() }

        // Act
        composeRule.setContent {
            ScreenProblemMessage(problem = unavailable, retry = {}, retryBorderWidth = GIVEN_BORDER_WIDTH)
        }
        val retry = composeRule.onNodeWithText(text = RETRY_LABEL).captureToImage().toPixelMap()
        val contentColor = retry[retry.width / 2, 0]
        val thickness = (0 until retry.height).takeWhile { y -> retry[retry.width / 2, y] == contentColor }.size

        // Assert
        assertTrue(actual = abs(thickness - expectedPx) <= ROUNDING_TOLERANCE_IN_PX)
    }

    @Test
    fun given_a_larger_text_style__when_shown__then_the_message_is_taller() {
        // Arrange
        var textStyle by mutableStateOf(value = ScreenProblemMessageDefaults.MessageTextStyle)
        composeRule.setContent {
            ScreenProblemMessage(problem = unavailable, retry = {}, textStyle = textStyle)
        }

        // Act
        val defaultHeight = heightOf(text = REASON)
        composeRule.runOnIdle { textStyle = textStyle.copy(fontSize = GIVEN_LARGE_FONT_SIZE) }
        val largerHeight = heightOf(text = REASON)

        // Assert
        assertTrue(actual = largerHeight > defaultHeight)
    }

    private fun Modifier.sized(): Modifier = size(width = GIVEN_WIDTH, height = GIVEN_HEIGHT)

    private fun heightOf(text: String): Dp {
        val bounds = composeRule.onNodeWithText(text = text).getBoundsInRoot()
        return bounds.bottom - bounds.top
    }

}
