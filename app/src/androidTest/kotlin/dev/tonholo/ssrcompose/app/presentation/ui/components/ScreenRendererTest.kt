package dev.tonholo.ssrcompose.app.presentation.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import dev.tonholo.ssrcompose.protocol.ActionId
import dev.tonholo.ssrcompose.protocol.ColumnNode
import dev.tonholo.ssrcompose.protocol.CounterCard as CounterCardContract
import dev.tonholo.ssrcompose.protocol.CounterCardProps
import dev.tonholo.ssrcompose.protocol.GreetingCard as GreetingCardContract
import dev.tonholo.ssrcompose.protocol.GreetingCardProps
import dev.tonholo.ssrcompose.protocol.ReferenceNode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test

class ScreenRendererTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val registry = ComponentRegistry().apply {
        register(GreetingCardContract) { props, _ -> GreetingCard(props = props) }
        register(CounterCardContract) { props, onAction -> CounterCard(props = props, onAction = onAction) }
    }

    private fun greetingReference(props: JsonObject) =
        ReferenceNode(componentId = GreetingCardContract.id, props = props)

    private val validProps = Json.encodeToJsonElement(
        GreetingCardProps.serializer(),
        GreetingCardProps(title = "Hello title", message = "Hello message"),
    ) as JsonObject

    @Test
    fun given_a_tree_with_a_greeting_card_reference__when_it_is_rendered__then_title_and_message_show() {
        // Arrange
        val tree = ColumnNode(children = listOf(greetingReference(validProps)))

        // Act
        composeRule.setContent { ScreenRenderer(node = tree, registry = registry, onAction = {}) }

        // Assert
        composeRule.onNodeWithText("Hello title").assertIsDisplayed()
        composeRule.onNodeWithText("Hello message").assertIsDisplayed()
    }

    @Test
    fun given_an_unknown_id_next_to_a_known_one__when_rendered__then_the_placeholder_and_the_card_show() {
        // Arrange
        val unknown = ReferenceNode(componentId = "mystery", props = JsonObject(emptyMap()))
        val tree = ColumnNode(children = listOf(unknown, greetingReference(validProps)))

        // Act
        composeRule.setContent { ScreenRenderer(node = tree, registry = registry, onAction = {}) }

        // Assert
        composeRule.onNodeWithText("Unknown component: mystery").assertIsDisplayed()
        composeRule.onNodeWithText("Hello title").assertIsDisplayed()
    }

    @Test
    fun given_a_greeting_card_reference_with_a_missing_prop__when_rendered__then_the_placeholder_shows() {
        // Arrange
        val brokenProps = buildJsonObject { put("title", "only a title") }
        val tree = ColumnNode(children = listOf(greetingReference(brokenProps)))

        // Act
        composeRule.setContent { ScreenRenderer(node = tree, registry = registry, onAction = {}) }

        // Assert
        composeRule.onNodeWithText("Unknown component: greeting-card").assertIsDisplayed()
    }

    private fun counterReference(count: Int): ReferenceNode {
        val props = Json.encodeToJsonElement(
            CounterCardProps.serializer(),
            CounterCardProps(title = "Counter", count = count, onAddOne = ActionId("add-one")),
        ) as JsonObject
        return ReferenceNode(componentId = CounterCardContract.id, props = props)
    }

    @Test
    fun given_a_counter_card_with_count_3__when_rendered__then_count_3_is_shown() {
        // Arrange
        val tree = ColumnNode(children = listOf(counterReference(count = 3)))

        // Act
        composeRule.setContent { ScreenRenderer(node = tree, registry = registry, onAction = {}) }

        // Assert
        composeRule.onNodeWithText("Counter").assertIsDisplayed()
        composeRule.onNodeWithText("Count: 3").assertIsDisplayed()
    }

    @Test
    fun given_a_counter_card__when_add_one_is_clicked__then_on_action_gets_its_id() {
        // Arrange
        val tree = ColumnNode(children = listOf(counterReference(count = 0)))
        val received = mutableListOf<ActionId>()
        composeRule.setContent { ScreenRenderer(node = tree, registry = registry, onAction = { received += it }) }

        // Act
        composeRule.onNodeWithText("Add one").performClick()

        // Assert
        assertEquals(expected = listOf(ActionId("add-one")), actual = received)
    }
}
