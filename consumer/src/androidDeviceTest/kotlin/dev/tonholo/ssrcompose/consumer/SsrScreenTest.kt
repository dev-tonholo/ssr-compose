package dev.tonholo.ssrcompose.consumer

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import dev.tonholo.ssrcompose.consumer.testing.APP
import dev.tonholo.ssrcompose.consumer.testing.AMOUNT
import dev.tonholo.ssrcompose.consumer.testing.CATALOG_VERSION
import dev.tonholo.ssrcompose.consumer.testing.CENTS
import dev.tonholo.ssrcompose.consumer.testing.FakeConnection
import dev.tonholo.ssrcompose.consumer.testing.MarkedStringResourceManager
import dev.tonholo.ssrcompose.consumer.testing.PAY_NOW
import dev.tonholo.ssrcompose.consumer.testing.SCREEN
import dev.tonholo.ssrcompose.consumer.testing.ScreenRobot
import dev.tonholo.ssrcompose.consumer.testing.contentOf
import dev.tonholo.ssrcompose.consumer.testing.greeting
import dev.tonholo.ssrcompose.consumer.text.LocalSsrComposeStringResourceManager
import dev.tonholo.ssrcompose.consumer.text.SsrComposeStringResource
import dev.tonholo.ssrcompose.core.data.DataValue
import dev.tonholo.ssrcompose.core.handshake.Answer
import dev.tonholo.ssrcompose.core.handshake.RefusalKind
import dev.tonholo.ssrcompose.core.message.PlacedComponent
import dev.tonholo.ssrcompose.core.message.ProducerMessage
import dev.tonholo.ssrcompose.core.message.ScreenContent
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test

private const val REFUSAL_REASON = "Please update the app"
private const val DEFAULT_RETRY_LABEL = "Try again"
private const val LARGE_SCREEN_COMPONENTS = 5_000
private const val LARGE_SCREEN_WAIT_MILLIS = 120_000L
private const val AMOUNT_CENTS = 1_250L
private const val CHANGED_TITLE = "Changed title"
private const val SCREENS_IN_A_ROW = 20
private const val SCREEN_TITLE_PREFIX = "Screen "
private const val QUARTER = 4

private val SIZED_WIDTH = 320.dp
private val SIZED_HEIGHT = 480.dp

private val REFUSAL = Answer.Refused(kind = RefusalKind.CatalogVersionNotSupported, reason = REFUSAL_REASON)

class SsrScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val robot = ScreenRobot(composeRule = composeRule)

    @Test
    fun given_a_screen_with_a_placed_component__when_shown__then_the_app_composable_draws_the_values() {
        // Arrange
        val connection = FakeConnection(content = contentOf(greeting()))

        // Act
        composeRule.setContent { robot.Screen(connection = { connection }) }
        robot.waitForText(text = PAY_NOW)

        // Assert
        composeRule.onNodeWithText(text = PAY_NOW).assertExists()
    }

    @Test
    fun given_an_app_manager_above_ssr_screen__when_the_connection_ends__then_the_default_message_shows_the_app_text() {
        // Arrange
        val connection = FakeConnection(content = contentOf(greeting()))
        val app = MarkedStringResourceManager
        composeRule.setContent {
            CompositionLocalProvider(LocalSsrComposeStringResourceManager provides app) {
                SsrScreen(
                    screen = SCREEN,
                    bindings = robot.bindings,
                    catalogVersion = CATALOG_VERSION,
                    connection = { connection },
                    app = APP,
                )
            }
        }
        robot.waitForText(text = PAY_NOW)

        // Act
        connection.end()
        robot.waitForText(text = app.markedTextOf(resource = SsrComposeStringResource.ScreenProblemConnectionEnded))

        // Assert
        composeRule.onNodeWithText(text = app.markedTextOf(resource = SsrComposeStringResource.ScreenProblemRetry))
            .assertExists()
    }

    @Test
    fun given_screens_decoded_off_the_main_thread__when_they_arrive__then_the_session_continues_on_the_main_thread() {
        // Arrange
        val connection = FakeConnection(content = contentOf(greeting()))
        composeRule.setContent { robot.Screen(connection = { connection }) }
        robot.waitForText(text = PAY_NOW)

        // Act
        repeat(times = SCREENS_IN_A_ROW) { index ->
            val content = contentOf(greeting(title = "$SCREEN_TITLE_PREFIX$index"))
            connection.deliver(message = ProducerMessage.Screen(screen = SCREEN, content = content))
        }
        robot.waitForText(text = "$SCREEN_TITLE_PREFIX${SCREENS_IN_A_ROW - 1}")

        // Assert
        assertEquals(expected = 0, actual = connection.framesTakenOffMain)
    }

    @Test
    fun given_a_readable_component__when_shown__then_it_is_placed_and_drawn() {
        // Arrange
        val values = mapOf(CENTS to DataValue.WholeNumber(value = AMOUNT_CENTS))
        val connection = FakeConnection(content = contentOf(PlacedComponent(name = AMOUNT, values = values)))

        // Act
        composeRule.setContent { robot.Screen(connection = { connection }) }
        robot.waitForText(text = AMOUNT_CENTS.toString())
        composeRule.waitForIdle()

        // Assert
        assertTrue(actual = robot.placements > 0)
        assertTrue(actual = robot.draws > 0)
    }

    @Test
    fun given_a_refusal__when_retry_is_tapped_on_the_default_message__then_the_app_is_introduced_again() {
        // Arrange
        val connections = ArrayDeque(
            listOf(
                FakeConnection(content = contentOf(greeting()), refusal = REFUSAL),
                FakeConnection(content = contentOf(greeting())),
            ),
        )
        composeRule.setContent {
            SsrScreen(
                screen = SCREEN,
                bindings = robot.bindings,
                catalogVersion = CATALOG_VERSION,
                connection = { connections.removeFirst() },
                app = APP,
            )
        }
        robot.waitForText(text = REFUSAL_REASON)

        // Act
        composeRule.onNodeWithText(text = DEFAULT_RETRY_LABEL).performClick()
        robot.waitForText(text = PAY_NOW)

        // Assert
        composeRule.onNodeWithText(text = REFUSAL_REASON).assertDoesNotExist()
    }

    @Test
    fun given_a_refusal_in_a_sized_screen__when_the_default_message_shows__then_it_is_centred_in_that_size() {
        // Arrange
        val connection = FakeConnection(content = contentOf(greeting()), refusal = REFUSAL)

        // Act
        composeRule.setContent {
            SsrScreen(
                screen = SCREEN,
                bindings = robot.bindings,
                catalogVersion = CATALOG_VERSION,
                connection = { connection },
                app = APP,
                modifier = Modifier.size(width = SIZED_WIDTH, height = SIZED_HEIGHT),
            )
        }
        robot.waitForText(text = REFUSAL_REASON)

        // Assert
        val reason = composeRule.onNodeWithText(text = REFUSAL_REASON).getBoundsInRoot()
        val retry = composeRule.onNodeWithText(text = DEFAULT_RETRY_LABEL).getBoundsInRoot()
        assertTrue(actual = reason.top >= SIZED_HEIGHT / QUARTER)
        assertTrue(actual = SIZED_HEIGHT - retry.bottom >= SIZED_HEIGHT / QUARTER)
    }

    @Test
    fun given_a_shown_screen__when_it_leaves_the_composition__then_the_connection_closes() {
        // Arrange
        val connection = FakeConnection(content = contentOf(greeting()))
        var shown by mutableStateOf(value = true)
        composeRule.setContent {
            if (shown) robot.Screen(connection = { connection })
        }
        robot.waitForText(text = PAY_NOW)

        // Act
        shown = false
        robot.waitForClosed(connection = connection)

        // Assert
        assertTrue(actual = connection.closed)
    }

    @Test
    fun given_thousands_of_components__when_shown_and_updated__then_the_last_one_is_composed() {
        // Arrange
        val lastPosition = LARGE_SCREEN_COMPONENTS - 1
        val components = List(size = LARGE_SCREEN_COMPONENTS) { index -> greeting(title = "Item$index") }
        val connection = FakeConnection(content = ScreenContent(components = components))

        // Act
        composeRule.setContent { robot.Screen(connection = { connection }) }
        robot.waitForText(text = "Item$lastPosition", timeoutMillis = LARGE_SCREEN_WAIT_MILLIS)
        val updated = ScreenContent(components = components.dropLast(n = 1) + greeting(title = CHANGED_TITLE))
        connection.deliver(message = ProducerMessage.Screen(screen = SCREEN, content = updated))
        robot.waitForText(text = CHANGED_TITLE, timeoutMillis = LARGE_SCREEN_WAIT_MILLIS)

        // Assert
        composeRule.onNodeWithText(text = CHANGED_TITLE).assertExists()
    }
}
