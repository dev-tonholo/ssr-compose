package dev.tonholo.ssrcompose.consumer

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import dev.tonholo.ssrcompose.consumer.testing.AMOUNT
import dev.tonholo.ssrcompose.consumer.testing.CENTS
import dev.tonholo.ssrcompose.consumer.testing.DIALOG_AMOUNT
import dev.tonholo.ssrcompose.consumer.testing.LATER_AMOUNT
import dev.tonholo.ssrcompose.consumer.testing.LAZY_AMOUNT
import dev.tonholo.ssrcompose.consumer.testing.FakeConnection
import dev.tonholo.ssrcompose.consumer.testing.MISMATCH_PREFIX
import dev.tonholo.ssrcompose.consumer.testing.OTHER_AMOUNT
import dev.tonholo.ssrcompose.consumer.testing.PAY_NOW
import dev.tonholo.ssrcompose.consumer.testing.POPUP_AMOUNT
import dev.tonholo.ssrcompose.consumer.testing.SCREEN
import dev.tonholo.ssrcompose.consumer.testing.SCROLLED_AMOUNT
import dev.tonholo.ssrcompose.consumer.testing.ScreenRobot
import dev.tonholo.ssrcompose.consumer.testing.contentOf
import dev.tonholo.ssrcompose.consumer.testing.greeting
import dev.tonholo.ssrcompose.core.data.DataValue
import dev.tonholo.ssrcompose.core.message.PlacedComponent
import dev.tonholo.ssrcompose.core.message.ProducerMessage
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

private const val UNKNOWN = "Unknown"
private const val SCROLLED_CENTS = 4_321L
private const val DIALOG_PLACEMENTS_UNTIL_ISSUE_189 = 2
private const val DIALOG_DRAWS_UNTIL_ISSUE_189 = 2
private const val POPUP_PLACEMENTS_UNTIL_ISSUE_189 = 1
private const val POPUP_DRAWS_UNTIL_ISSUE_189 = 1

class SsrScreenProblemTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val robot = ScreenRobot(composeRule = composeRule)

    @Test
    fun given_a_failed_read__when_shown__then_the_stand_in_is_never_placed_or_drawn() {
        // Arrange
        val connection = FakeConnection(content = contentOf(PlacedComponent(name = AMOUNT)))

        // Act
        composeRule.setContent { robot.Screen(connection = { connection }) }
        robot.waitForText(text = MISMATCH_PREFIX + AMOUNT)
        composeRule.waitForIdle()

        // Assert
        assertEquals(expected = 0, actual = robot.placements)
        assertEquals(expected = 0, actual = robot.draws)
    }

    @Test
    fun given_two_failed_reads__when_shown__then_the_topmost_component_is_the_mismatch() {
        // Arrange
        val content = contentOf(greeting(), PlacedComponent(name = OTHER_AMOUNT), PlacedComponent(name = AMOUNT))
        val connection = FakeConnection(content = content)

        // Act
        composeRule.setContent { robot.Screen(connection = { connection }) }
        robot.waitForText(text = MISMATCH_PREFIX + OTHER_AMOUNT)

        // Assert
        composeRule.onNodeWithText(text = MISMATCH_PREFIX + OTHER_AMOUNT).assertExists()
    }

    @Test
    fun given_an_unknown_component__when_shown__then_it_is_the_mismatch() {
        // Arrange
        val connection = FakeConnection(content = contentOf(PlacedComponent(name = UNKNOWN)))

        // Act
        composeRule.setContent { robot.Screen(connection = { connection }) }
        robot.waitForText(text = MISMATCH_PREFIX + UNKNOWN)

        // Assert
        composeRule.onNodeWithText(text = MISMATCH_PREFIX + UNKNOWN).assertExists()
    }

    @Test
    fun given_a_mismatch__when_the_next_screen_arrives__then_it_is_shown() {
        // Arrange
        val connection = FakeConnection(content = contentOf(PlacedComponent(name = AMOUNT)))
        composeRule.setContent { robot.Screen(connection = { connection }) }
        robot.waitForText(text = MISMATCH_PREFIX + AMOUNT)

        // Act
        connection.deliver(message = ProducerMessage.Screen(screen = SCREEN, content = contentOf(greeting())))
        robot.waitForText(text = PAY_NOW)

        // Assert
        composeRule.onNodeWithText(text = MISMATCH_PREFIX + AMOUNT).assertDoesNotExist()
    }

    @Test
    fun given_a_read_that_fails_on_a_later_recomposition__when_shown__then_the_stand_in_is_never_placed_or_drawn() {
        // Arrange
        val connection = FakeConnection(content = contentOf(PlacedComponent(name = LATER_AMOUNT)))

        // Act
        composeRule.setContent { robot.Screen(connection = { connection }) }
        robot.waitForText(text = MISMATCH_PREFIX + LATER_AMOUNT)
        composeRule.waitForIdle()

        // Assert
        assertEquals(expected = 0, actual = robot.placements)
        assertEquals(expected = 0, actual = robot.draws)
    }

    @Test
    fun given_a_failed_read_in_a_lazy_list_item__when_shown__then_the_stand_in_is_never_placed_or_drawn() {
        // Arrange
        val connection = FakeConnection(content = contentOf(PlacedComponent(name = LAZY_AMOUNT)))

        // Act
        composeRule.setContent { robot.Screen(connection = { connection }) }
        robot.waitForText(text = MISMATCH_PREFIX + LAZY_AMOUNT)
        composeRule.waitForIdle()

        // Assert
        assertEquals(expected = 0, actual = robot.placements)
        assertEquals(expected = 0, actual = robot.draws)
    }

    @Test
    fun given_a_failed_read_in_a_scrolled_item__when_shown__then_the_problem_replaces_the_screen_before_it_is_drawn() {
        // Arrange
        val connection = FakeConnection(content = contentOf(PlacedComponent(name = SCROLLED_AMOUNT)))

        // Act
        composeRule.setContent { robot.Screen(connection = { connection }) }
        robot.waitForText(text = MISMATCH_PREFIX + SCROLLED_AMOUNT)
        composeRule.waitForIdle()

        // Assert
        assertEquals(expected = 0, actual = robot.draws)
    }

    @Test
    fun given_a_readable_item_scrolled_into_view__when_shown__then_it_is_drawn() {
        // Arrange
        val values = mapOf(CENTS to DataValue.WholeNumber(value = SCROLLED_CENTS))
        val connection = FakeConnection(content = contentOf(PlacedComponent(name = SCROLLED_AMOUNT, values = values)))

        // Act
        composeRule.setContent { robot.Screen(connection = { connection }) }
        robot.waitForText(text = SCROLLED_CENTS.toString())
        composeRule.waitForIdle()

        // Assert
        assertTrue(actual = robot.draws > 0)
    }

    @Test
    fun given_a_failed_read_in_a_dialog__when_shown__then_the_stand_in_shows_as_the_known_limit_of_issue_189() {
        // Arrange
        val connection = FakeConnection(content = contentOf(PlacedComponent(name = DIALOG_AMOUNT)))

        // Act
        composeRule.setContent { robot.Screen(connection = { connection }) }
        robot.waitForText(text = MISMATCH_PREFIX + DIALOG_AMOUNT)
        composeRule.waitForIdle()

        // Assert
        assertEquals(expected = DIALOG_PLACEMENTS_UNTIL_ISSUE_189, actual = robot.placements)
        assertEquals(expected = DIALOG_DRAWS_UNTIL_ISSUE_189, actual = robot.draws)
    }

    @Test
    fun given_a_failed_read_in_a_popup__when_shown__then_the_stand_in_shows_as_the_known_limit_of_issue_189() {
        // Arrange
        val connection = FakeConnection(content = contentOf(PlacedComponent(name = POPUP_AMOUNT)))

        // Act
        composeRule.setContent { robot.Screen(connection = { connection }) }
        robot.waitForText(text = MISMATCH_PREFIX + POPUP_AMOUNT)
        composeRule.waitForIdle()

        // Assert
        assertEquals(expected = POPUP_PLACEMENTS_UNTIL_ISSUE_189, actual = robot.placements)
        assertEquals(expected = POPUP_DRAWS_UNTIL_ISSUE_189, actual = robot.draws)
    }
}
