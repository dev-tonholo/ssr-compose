package dev.tonholo.ssrcompose.consumer.problem

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import dev.tonholo.ssrcompose.consumer.ScreenProblem
import dev.tonholo.ssrcompose.consumer.testing.GIVEN_HEIGHT
import dev.tonholo.ssrcompose.consumer.testing.GIVEN_WIDTH
import dev.tonholo.ssrcompose.consumer.testing.REASON
import dev.tonholo.ssrcompose.consumer.testing.RETRY_LABEL
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

private val GIVEN_CONTAINER = Color(color = 0xFFD81B60)
private val GIVEN_CONTENT = Color(color = 0xFF00897B)
private val GIVEN_TEXT_STYLE_COLOR = Color(color = 0xFFFFB300)

class ScreenProblemMessageColoringTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val unavailable = ScreenProblem.Unavailable(reason = REASON)

    @Test
    fun given_colors_the_defaults_never_use__when_shown__then_the_container_fills_the_image() {
        // Arrange
        val colors = ScreenProblemMessageColors(containerColor = GIVEN_CONTAINER, contentColor = GIVEN_CONTENT)

        // Act
        composeRule.setContent {
            ScreenProblemMessage(problem = unavailable, retry = {}, modifier = Modifier.sized(), colors = colors)
        }
        val image = composeRule.onRoot().captureToImage().toPixelMap()

        // Assert
        assertEquals(expected = GIVEN_CONTAINER, actual = image[0, 0])
    }

    @Test
    fun given_colors_the_defaults_never_use__when_shown__then_the_content_color_is_in_the_text() {
        // Arrange
        val colors = ScreenProblemMessageColors(containerColor = GIVEN_CONTAINER, contentColor = GIVEN_CONTENT)

        // Act
        composeRule.setContent {
            ScreenProblemMessage(problem = unavailable, retry = {}, colors = colors)
        }
        val text = composeRule.onNodeWithText(text = REASON).captureToImage().toPixelMap()

        // Assert
        assertTrue(actual = text.contains(color = GIVEN_CONTENT))
    }

    @Test
    fun given_a_content_color__when_shown__then_the_retry_border_has_it() {
        // Arrange
        val colors = ScreenProblemMessageColors(containerColor = GIVEN_CONTAINER, contentColor = GIVEN_CONTENT)

        // Act
        composeRule.setContent {
            ScreenProblemMessage(problem = unavailable, retry = {}, colors = colors)
        }
        val retry = composeRule.onNodeWithText(text = RETRY_LABEL).captureToImage().toPixelMap()

        // Assert
        assertEquals(expected = GIVEN_CONTENT, actual = retry[retry.width / 2, 0])
    }

    @Test
    fun given_a_text_style_with_its_own_color__when_shown__then_the_content_color_colors_the_text() {
        // Arrange
        val colors = ScreenProblemMessageColors(containerColor = GIVEN_CONTAINER, contentColor = GIVEN_CONTENT)
        val textStyle = ScreenProblemMessageDefaults.MessageTextStyle.copy(color = GIVEN_TEXT_STYLE_COLOR)

        // Act
        composeRule.setContent {
            ScreenProblemMessage(problem = unavailable, retry = {}, colors = colors, textStyle = textStyle)
        }
        val text = composeRule.onNodeWithText(text = REASON).captureToImage().toPixelMap()

        // Assert
        assertTrue(actual = text.contains(color = GIVEN_CONTENT))
        assertFalse(actual = text.contains(color = GIVEN_TEXT_STYLE_COLOR))
    }

    private fun Modifier.sized(): Modifier = size(width = GIVEN_WIDTH, height = GIVEN_HEIGHT)

    private fun PixelMap.contains(color: Color): Boolean =
        (0 until width).any { x -> (0 until height).any { y -> this[x, y] == color } }
}
