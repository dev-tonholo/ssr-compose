package dev.tonholo.ssrcompose.consumer.problem

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import dev.tonholo.ssrcompose.consumer.ScreenProblem
import dev.tonholo.ssrcompose.consumer.testing.GIVEN_HEIGHT
import dev.tonholo.ssrcompose.consumer.testing.GIVEN_WIDTH
import dev.tonholo.ssrcompose.consumer.testing.LONG_REASON
import dev.tonholo.ssrcompose.consumer.testing.MarkedStringResourceManager
import dev.tonholo.ssrcompose.consumer.testing.REASON
import dev.tonholo.ssrcompose.consumer.testing.RETRY_LABEL
import dev.tonholo.ssrcompose.consumer.text.LocalSsrComposeStringResourceManager
import dev.tonholo.ssrcompose.consumer.text.SsrComposeStringResource
import dev.tonholo.ssrcompose.core.handshake.RefusalKind
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

private const val CONNECTION_ENDED_TEXT_FROM_CONTRACT = "The connection to the server ended. Please try again."
private const val ANSWER_UNREADABLE_TEXT_FROM_CONTRACT = "The app could not read the answer from the server."

private const val MISMATCHED_COMPONENT = "Amount"
private val MINIMUM_INSET = 16.dp
private val MINIMUM_GAP = 8.dp

class ScreenProblemMessageTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun given_more_space_than_the_message__when_it_shows__then_it_fills_it_with_text_and_button_apart() {
        // Arrange
        composeRule.setContent {
            ScreenProblemMessage(
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
            ScreenProblemMessage(problem = ScreenProblem.ConnectionEnded, retry = {})
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
            ScreenProblemMessage(problem = ScreenProblem.AnswerUnreadable, retry = {})
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
                ScreenProblemMessage(problem = problem, retry = {})
            }
        }

        // Act
        val shown = keyOfEachProblem.associate { (next, key) ->
            composeRule.runOnIdle { problem = next }
            key to (isShown(key = key) && isShown(key = SsrComposeStringResource.ScreenProblemRetry))
        }

        // Assert
        assertEquals(expected = keyOfEachProblem.associate { (_, key) -> key to true }, actual = shown)
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
                ScreenProblemMessage(problem = problem, retry = {})
            }
        }
    }

    private fun isShown(key: SsrComposeStringResource): Boolean {
        val text = MarkedStringResourceManager.markedTextOf(resource = key)
        return composeRule.onAllNodesWithText(text = text).fetchSemanticsNodes().isNotEmpty() &&
            composeRule.onNodeWithText(text = text).isDisplayed()
    }
}
