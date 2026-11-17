package dev.tonholo.ssrcompose.producer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.tonholo.ssrcompose.producer.experiment.first.Greeting as FirstGreeting
import dev.tonholo.ssrcompose.producer.experiment.second.Greeting as SecondGreeting
import dev.tonholo.ssrcompose.producer.internal.PlacedNode
import dev.tonholo.ssrcompose.producer.testing.LiveComposition
import dev.tonholo.ssrcompose.producer.testing.composeOnce
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class BridgeExperimentTest {
    @Test
    fun `given a created bridge with every parameter, when composed, then the node holds its version and values`() {
        // Arrange
        val expectedValues = mapOf(TITLE to TITLE_TEXT, SUBTITLE to SUBTITLE_TEXT)

        // Act
        val root = composeOnce {
            FirstGreeting(title = TITLE_TEXT, onSelect = {}, subtitle = SUBTITLE_TEXT) {
                FirstGreeting(title = CHILD_TITLE_TEXT, onSelect = {})
            }
        }

        // Assert
        val greeting = root.children.single() as PlacedNode.Component
        assertEquals(expected = COMPONENT_NAME, actual = greeting.name)
        assertEquals(expected = FIRST_VERSION, actual = greeting.catalogVersion)
        assertEquals(expected = expectedValues, actual = greeting.values)
        val slot = greeting.children.single() as PlacedNode.Slot
        assertEquals(expected = CONTENT, actual = slot.name)
        val child = slot.children.single() as PlacedNode.Component
        assertEquals(expected = mapOf(TITLE to CHILD_TITLE_TEXT), actual = child.values)
    }

    @Test
    fun `given two created bridges with one component name, when composed, then each node passes its own version`() {
        // Arrange
        val expectedVersions = listOf(FIRST_VERSION, SECOND_VERSION)

        // Act
        val root = composeOnce {
            FirstGreeting(title = TITLE_TEXT, onSelect = {})
            SecondGreeting(title = TITLE_TEXT, onSelect = {}, badge = BADGE_TEXT)
        }

        // Assert
        val greetings = root.children.map { it as PlacedNode.Component }
        assertEquals(expected = expectedVersions, actual = greetings.map { it.catalogVersion })
        assertEquals(expected = mapOf(TITLE to TITLE_TEXT), actual = greetings[0].values)
        assertEquals(expected = mapOf(TITLE to TITLE_TEXT, BADGE to BADGE_TEXT), actual = greetings[1].values)
    }

    @Test
    fun `given a created bridge with optional parameters left out, when composed, then they are absent`() {
        // Arrange
        val expectedValues = mapOf(TITLE to TITLE_TEXT)

        // Act
        val root = composeOnce { FirstGreeting(title = TITLE_TEXT, onSelect = {}) }

        // Assert
        val greeting = root.children.single() as PlacedNode.Component
        assertEquals(expected = expectedValues, actual = greeting.values)
        assertTrue(actual = greeting.children.isEmpty())
    }

    @Test
    fun `given a lambda for a created RemoteAction1, when the placed action runs, then the lambda gets the argument`() {
        // Arrange
        var received = ""

        // Act
        val root = composeOnce {
            FirstGreeting(title = TITLE_TEXT, onSelect = { selected -> received = selected })
        }
        val greeting = root.children.single() as PlacedNode.Component
        greeting.actions.getValue(ON_SELECT).invoke(SELECTED_TEXT)

        // Assert
        assertEquals(expected = SELECTED_TEXT, actual = received)
    }

    @Test
    fun `given a created bridge reading state, when the state changes, then recomposition updates the node`() =
        runTest {
            // Arrange
            var title by mutableStateOf(TITLE_TEXT)
            val live = LiveComposition(scope = this) {
                FirstGreeting(title = title, onSelect = {})
            }

            // Act
            title = CHANGED_TITLE_TEXT
            live.recompose()

            // Assert
            val greeting = live.root.children.single() as PlacedNode.Component
            assertEquals(expected = mapOf(TITLE to CHANGED_TITLE_TEXT), actual = greeting.values)
            live.dispose()
        }

    private companion object {
        const val FIRST_VERSION = "1"
        const val SECOND_VERSION = "2"
        const val COMPONENT_NAME = "Greeting"
        const val TITLE = "title"
        const val SUBTITLE = "subtitle"
        const val BADGE = "badge"
        const val ON_SELECT = "onSelect"
        const val CONTENT = "content"
        const val TITLE_TEXT = "Welcome"
        const val SUBTITLE_TEXT = "Details"
        const val BADGE_TEXT = "New"
        const val CHILD_TITLE_TEXT = "Nested"
        const val CHANGED_TITLE_TEXT = "Updated"
        const val SELECTED_TEXT = "clicked"
    }
}
