package dev.tonholo.ssrcompose.app.presentation.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import kotlin.random.Random
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
        composeRule.setContent { FragmentPlayer(document = document) }

        // Assert
        composeRule.onNodeWithText(PLACEHOLDER).assertIsDisplayed()
    }

    @Test
    fun given_a_fragment_with_random_bytes__when_rendered__then_the_placeholder_is_shown() {
        // Arrange
        val document = Random(seed = RANDOM_SEED).nextBytes(size = RANDOM_BYTE_COUNT)

        // Act
        composeRule.setContent { FragmentPlayer(document = document) }

        // Assert
        composeRule.onNodeWithText(PLACEHOLDER).assertIsDisplayed()
    }

    @Test
    fun given_a_valid_document__when_rendered__then_the_fragment_player_node_is_displayed() {
        // Arrange
        val document = boxDocument(widthPx = DOCUMENT_WIDTH_PX, heightPx = DOCUMENT_HEIGHT_PX)

        // Act
        composeRule.setContent { FragmentPlayer(document = document) }

        // Assert
        composeRule.onNodeWithTag("fragment-player").assertIsDisplayed()
    }
}
