package dev.tonholo.ssrcompose.app.presentation.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.tonholo.ssrcompose.protocol.ActionId
import kotlin.test.assertEquals
import kotlin.random.Random
import dev.tonholo.ssrcompose.protocol.FragmentNode
import org.junit.Rule
import org.junit.Test

private const val PLACEHOLDER = "Could not play fragment"
private const val DOCUMENT_WIDTH_PX = 200
private const val DOCUMENT_HEIGHT_PX = 100
private const val RANDOM_BYTE_COUNT = 256
private const val RANDOM_SEED = 7

class FragmentPlayerTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun given_a_fragment_with_empty_bytes__when_rendered__then_the_placeholder_is_shown() {
        // Arrange
        val document = ByteArray(size = 0)

        // Act
        composeRule.setContent { FragmentPlayer(fragment = FragmentNode(document = document), onAction = {}) }

        // Assert
        composeRule.onNodeWithText(PLACEHOLDER).assertIsDisplayed()
    }

    @Test
    fun given_a_fragment_with_random_bytes__when_rendered__then_the_placeholder_is_shown() {
        // Arrange
        val document = Random(seed = RANDOM_SEED).nextBytes(size = RANDOM_BYTE_COUNT)

        // Act
        composeRule.setContent { FragmentPlayer(fragment = FragmentNode(document = document), onAction = {}) }

        // Assert
        composeRule.onNodeWithText(PLACEHOLDER).assertIsDisplayed()
    }

    @Test
    fun given_a_valid_document__when_rendered__then_the_fragment_player_node_is_displayed() {
        // Arrange
        val document = boxDocument(widthPx = DOCUMENT_WIDTH_PX, heightPx = DOCUMENT_HEIGHT_PX)

        // Act
        composeRule.setContent { FragmentPlayer(fragment = FragmentNode(document = document), onAction = {}) }

        // Assert
        composeRule.onNodeWithTag("fragment-player").assertIsDisplayed()
    }

    @Test
    fun given_a_host_action_tap_area__when_clicked__then_on_action_gets_the_action_id() {
        // Arrange
        val document = clickableBoxDocument(
            widthPx = DOCUMENT_WIDTH_PX,
            heightPx = DOCUMENT_HEIGHT_PX,
            actionId = "test-action",
        )
        val received = mutableListOf<ActionId>()
        composeRule.setContent {
            FragmentPlayer(fragment = FragmentNode(document = document), onAction = { received += it })
        }

        // Act
        composeRule.onNodeWithTag("fragment-player").performClick()
        composeRule.waitForIdle()

        // Assert
        assertEquals(expected = listOf(ActionId("test-action")), actual = received)
    }

    @Test
    fun given_an_equal_fragment_in_a_new_tree__when_recomposed__then_the_document_is_not_parsed_again() {
        // Arrange
        val bytes = boxDocument(widthPx = DOCUMENT_WIDTH_PX, heightPx = DOCUMENT_HEIGHT_PX)
        var parseCount = 0
        var tick by mutableIntStateOf(0)
        composeRule.setContent {
            FragmentPlayer(
                fragment = FragmentNode(document = bytes.copyOf().also { tick }),
                onAction = {},
                modifier = Modifier,
                parse = { document -> parseCount++.let { parseFragment(document) } },
            )
        }

        // Act
        composeRule.runOnIdle { tick++ }
        composeRule.waitForIdle()

        // Assert
        assertEquals(expected = 1, actual = parseCount)
    }
}
