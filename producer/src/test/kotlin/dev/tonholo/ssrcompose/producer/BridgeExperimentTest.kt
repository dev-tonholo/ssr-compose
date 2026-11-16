package dev.tonholo.ssrcompose.producer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.tonholo.ssrcompose.producer.internal.PlacedNode
import dev.tonholo.ssrcompose.producer.testing.LiveComposition
import dev.tonholo.ssrcompose.producer.testing.composeOnce
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class BridgeExperimentTest {
    @Test
    fun `given a hand-written bridge with every parameter, when composed, then the node holds all of them`() {
        // Arrange
        val expectedValues = mapOf(TITLE to TITLE_TEXT, SUBTITLE to SUBTITLE_TEXT)

        // Act
        val root = composeOnce {
            HandWrittenGreeting(
                title = TITLE_TEXT,
                onSelect = {},
                subtitle = SUBTITLE_TEXT,
                content = { HandWrittenGreeting(title = CHILD_TITLE_TEXT, onSelect = {}) },
            )
        }

        // Assert
        val greeting = root.children.single() as PlacedNode.Component
        assertEquals(expected = COMPONENT_NAME, actual = greeting.name)
        assertEquals(expected = CATALOG_VERSION, actual = greeting.catalogVersion)
        assertEquals(expected = expectedValues, actual = greeting.values)
        val slot = greeting.children.single() as PlacedNode.Slot
        assertEquals(expected = CONTENT, actual = slot.name)
        val child = slot.children.single() as PlacedNode.Component
        assertEquals(expected = mapOf(TITLE to CHILD_TITLE_TEXT), actual = child.values)
    }

    @Test
    fun `given a hand-written bridge with optional parameters left out, when composed, then they are absent`() {
        // Arrange
        val expectedValues = mapOf(TITLE to TITLE_TEXT)

        // Act
        val root = composeOnce { HandWrittenGreeting(title = TITLE_TEXT, onSelect = {}) }

        // Assert
        val greeting = root.children.single() as PlacedNode.Component
        assertEquals(expected = expectedValues, actual = greeting.values)
        assertTrue(actual = greeting.children.isEmpty())
    }

    @Test
    fun `given a lambda for a RemoteAction1, when the placed action runs, then the lambda gets the argument`() {
        // Arrange
        var received = ""

        // Act
        val root = composeOnce {
            HandWrittenGreeting(title = TITLE_TEXT, onSelect = { selected -> received = selected })
        }
        val greeting = root.children.single() as PlacedNode.Component
        greeting.actions.getValue(ON_SELECT).invoke(SELECTED_TEXT)

        // Assert
        assertEquals(expected = SELECTED_TEXT, actual = received)
    }

    @Test
    fun `given a bridge reading state, when the state changes, then recomposition updates the node`() =
        runTest {
            // Arrange
            var title by mutableStateOf(TITLE_TEXT)
            val live = LiveComposition(scope = this) {
                HandWrittenGreeting(title = title, onSelect = {})
            }

            // Act
            title = CHANGED_TITLE_TEXT
            live.recompose()

            // Assert
            val greeting = live.root.children.single() as PlacedNode.Component
            assertEquals(expected = mapOf(TITLE to CHANGED_TITLE_TEXT), actual = greeting.values)
            live.dispose()
        }

    @Composable
    private fun HandWrittenGreeting(
        title: String,
        onSelect: RemoteAction1<String>,
        subtitle: String? = null,
        content: (@Composable () -> Unit)? = null,
    ) {
        ConsumerComponent(
            catalogVersion = CATALOG_VERSION,
            name = COMPONENT_NAME,
            valueNames = arrayOf(TITLE, SUBTITLE),
            values = arrayOf(title, subtitle),
            actionNames = arrayOf(ON_SELECT),
            actions = arrayOf(onSelect),
            slotNames = arrayOf(CONTENT),
            slots = arrayOf(content),
        )
    }

    private companion object {
        const val CATALOG_VERSION = "hand-written"
        const val COMPONENT_NAME = "Greeting"
        const val TITLE = "title"
        const val SUBTITLE = "subtitle"
        const val ON_SELECT = "onSelect"
        const val CONTENT = "content"
        const val TITLE_TEXT = "Welcome"
        const val SUBTITLE_TEXT = "Details"
        const val CHILD_TITLE_TEXT = "Nested"
        const val CHANGED_TITLE_TEXT = "Updated"
        const val SELECTED_TEXT = "clicked"
    }
}
