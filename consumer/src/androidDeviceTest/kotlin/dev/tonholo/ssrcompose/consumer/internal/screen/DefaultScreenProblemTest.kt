package dev.tonholo.ssrcompose.consumer.internal.screen

import android.content.res.Configuration
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.tonholo.ssrcompose.consumer.ScreenProblem
import dev.tonholo.ssrcompose.consumer.testing.MarkedStringResourceManager
import dev.tonholo.ssrcompose.consumer.text.LocalSsrComposeStringResourceManager
import dev.tonholo.ssrcompose.consumer.text.SsrComposeStringResource
import dev.tonholo.ssrcompose.core.handshake.RefusalKind
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

private const val RETRY_LABEL = "Try again"
private const val REASON = "The screen is not available right now."
private const val CONNECTION_ENDED_TEXT_FROM_CONTRACT = "The connection to the server ended. Please try again."
private const val ANSWER_UNREADABLE_TEXT_FROM_CONTRACT = "The app could not read the answer from the server."
private const val LONG_REASON = "The screen is not available right now. Check the connection and try again in a moment."

private const val MISMATCHED_COMPONENT = "Amount"

private const val HALF_OF_THE_LUMINANCE_RANGE = 0.5f

private val TEXT_ONLY_INSET_OF_THE_REASON = 0.dp
private val TEXT_ONLY_INSET_OF_THE_LABEL = 6.dp
private val GIVEN_WIDTH = 320.dp
private val GIVEN_HEIGHT = 480.dp
private val MINIMUM_INSET = 16.dp
private val MINIMUM_GAP = 8.dp

class DefaultScreenProblemTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun given_a_dark_system_theme_on_a_light_window__when_the_message_shows__then_its_text_is_readable() {
        // Arrange
        composeRule.setContent {
            val dark = Configuration(LocalConfiguration.current).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_YES
            }
            CompositionLocalProvider(LocalConfiguration provides dark) {
                DefaultScreenProblem(problem = ScreenProblem.Unavailable(reason = REASON), retry = {})
            }
        }

        // Act
        val reasonContrast = luminanceRangeInside(text = REASON, inset = TEXT_ONLY_INSET_OF_THE_REASON)
        val labelContrast = luminanceRangeInside(text = RETRY_LABEL, inset = TEXT_ONLY_INSET_OF_THE_LABEL)

        // Assert
        assertTrue(actual = reasonContrast >= HALF_OF_THE_LUMINANCE_RANGE)
        assertTrue(actual = labelContrast >= HALF_OF_THE_LUMINANCE_RANGE)
    }

    @Test
    fun given_more_space_than_the_message__when_it_shows__then_it_fills_it_with_text_and_button_apart() {
        // Arrange
        composeRule.setContent {
            DefaultScreenProblem(
                problem = ScreenProblem.Unavailable(reason = LONG_REASON),
                retry = {},
                modifier = Modifier.size(width = GIVEN_WIDTH, height = GIVEN_HEIGHT),
            )
        }

        // Act
        val root = composeRule.onRoot().getBoundsInRoot()
        val reason = composeRule.onNodeWithText(text = LONG_REASON).getBoundsInRoot()
        val retry = composeRule.onNodeWithText(text = RETRY_LABEL).getBoundsInRoot()

        // Assert
        assertEquals(expected = GIVEN_WIDTH, actual = root.right - root.left)
        assertEquals(expected = GIVEN_HEIGHT, actual = root.bottom - root.top)
        assertTrue(actual = reason.left - root.left >= MINIMUM_INSET)
        assertTrue(actual = root.right - reason.right >= MINIMUM_INSET)
        assertTrue(actual = retry.top - reason.bottom >= MINIMUM_GAP)
    }

    @Test
    fun given_a_connection_that_ended__when_the_message_shows__then_it_says_so_in_english() {
        // Arrange
        composeRule.setContent {
            DefaultScreenProblem(problem = ScreenProblem.ConnectionEnded, retry = {})
        }

        // Act
        val message = composeRule.onNodeWithText(text = CONNECTION_ENDED_TEXT_FROM_CONTRACT)

        // Assert
        message.assertIsDisplayed()
    }

    @Test
    fun given_an_answer_the_app_cannot_read__when_the_message_shows__then_it_says_so_in_english() {
        // Arrange
        composeRule.setContent {
            DefaultScreenProblem(problem = ScreenProblem.AnswerUnreadable, retry = {})
        }

        // Act
        val message = composeRule.onNodeWithText(text = ANSWER_UNREADABLE_TEXT_FROM_CONTRACT)

        // Assert
        message.assertIsDisplayed()
    }

    @Test
    fun given_an_app_manager__when_each_library_written_problem_shows__then_it_shows_its_own_text_and_the_retry_text() {
        // Arrange
        val keyOfEachProblem = listOf(
            ScreenProblem.Mismatch(component = MISMATCHED_COMPONENT) to SsrComposeStringResource.ScreenProblemMismatch,
            ScreenProblem.ConnectionEnded to SsrComposeStringResource.ScreenProblemConnectionEnded,
            ScreenProblem.AnswerUnreadable to SsrComposeStringResource.ScreenProblemAnswerUnreadable,
        )
        var problem: ScreenProblem by mutableStateOf(value = keyOfEachProblem.first().first)
        composeRule.setContent {
            CompositionLocalProvider(LocalSsrComposeStringResourceManager provides MarkedStringResourceManager) {
                DefaultScreenProblem(problem = problem, retry = {})
            }
        }

        // Act and Assert
        keyOfEachProblem.forEach { (next, key) ->
            composeRule.runOnIdle { problem = next }
            assertDisplays(key = key)
            assertDisplays(key = SsrComposeStringResource.ScreenProblemRetry)
        }
    }

    @Test
    fun given_an_app_manager__when_an_unavailable_reason_shows__then_the_reason_is_shown_as_given() {
        // Arrange
        val problem = ScreenProblem.Unavailable(reason = REASON)

        // Act
        showWithAppManager(problem = problem)

        // Assert
        composeRule.onNodeWithText(text = REASON).assertIsDisplayed()
    }

    @Test
    fun given_an_app_manager__when_a_refusal_reason_shows__then_the_reason_is_shown_as_given() {
        // Arrange
        val problem = ScreenProblem.Refused(kind = RefusalKind.CatalogVersionNotSupported, reason = REASON)

        // Act
        showWithAppManager(problem = problem)

        // Assert
        composeRule.onNodeWithText(text = REASON).assertIsDisplayed()
    }

    private fun showWithAppManager(problem: ScreenProblem) {
        composeRule.setContent {
            CompositionLocalProvider(LocalSsrComposeStringResourceManager provides MarkedStringResourceManager) {
                DefaultScreenProblem(problem = problem, retry = {})
            }
        }
    }

    private fun assertDisplays(key: SsrComposeStringResource) {
        composeRule.onNodeWithText(text = MarkedStringResourceManager.markedTextOf(resource = key)).assertIsDisplayed()
    }

    /** The luminance range of the node with [text], leaving out a band of [inset] on each side. */
    private fun luminanceRangeInside(text: String, inset: Dp): Float {
        val pixels = composeRule.onNodeWithText(text = text).captureToImage().toPixelMap()
        val insetPx = with(composeRule.density) { inset.roundToPx() }
        val luminances = (insetPx until pixels.width - insetPx).flatMap { x ->
            (insetPx until pixels.height - insetPx).map { y -> pixels[x, y].luminance() }
        }
        return luminances.max() - luminances.min()
    }
}
