package dev.tonholo.ssrcompose.server.tree

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.cash.turbine.test
import dev.tonholo.ssrcompose.protocol.ColumnNode
import dev.tonholo.ssrcompose.protocol.GreetingCard
import dev.tonholo.ssrcompose.protocol.GreetingCardProps
import dev.tonholo.ssrcompose.protocol.ReferenceNode
import dev.tonholo.ssrcompose.protocol.ScreenNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class LiveTreeTest {
    private fun titleOf(tree: ScreenNode): String {
        val reference = assertIs<ReferenceNode>(assertIs<ColumnNode>(tree).children.single())
        return reference.props.getValue("title").toString().trim('"')
    }

    private fun card(title: String) = GreetingCardProps(title = title, message = "m")

    @Test
    fun `given content with state, when the state changes, then a new tree is emitted`() = runTest {
        // Arrange
        var title by mutableStateOf("one")
        val content: @Composable () -> Unit = { Reference(GreetingCard, card(title)) }

        liveTree(content).test {
            assertEquals(expected = "one", actual = titleOf(awaitItem()))

            // Act
            title = "two"

            // Assert
            assertEquals(expected = "two", actual = titleOf(awaitItem()))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given a delaying launched effect, when time passes, then the tree updates without any event`() =
        runTest {
            // Arrange
            val content: @Composable () -> Unit = {
                var title by remember { mutableStateOf("before") }
                LaunchedEffect(Unit) {
                    delay(EFFECT_DELAY_MS)
                    title = "after"
                }
                Reference(GreetingCard, card(title))
            }

            liveTree(content).test {
                assertEquals(expected = "before", actual = titleOf(awaitItem()))

                // Act
                val updated = awaitItem()

                // Assert
                assertEquals(expected = "after", actual = titleOf(updated))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `given a collector that cancels, when the flow ends, then the effect and composition end`() =
        runTest {
            // Arrange
            val events = mutableListOf<String>()
            val content: @Composable () -> Unit = {
                DisposableEffect(Unit) { onDispose { events += "disposed" } }
                LaunchedEffect(Unit) {
                    try {
                        delay(Long.MAX_VALUE)
                    } finally {
                        events += "cancelled"
                    }
                }
            }

            // Act
            liveTree(content).test {
                awaitItem()
                cancelAndIgnoreRemainingEvents()
            }

            // Assert
            assertEquals(expected = setOf("disposed", "cancelled"), actual = events.toSet())
        }

    @Test
    fun `given three state changes in a burst, when collected, then the last emitted tree holds the latest value`() =
        runTest {
            // Arrange
            var title by mutableStateOf("zero")
            val content: @Composable () -> Unit = { Reference(GreetingCard, card(title)) }

            liveTree(content).test {
                awaitItem()

                // Act
                title = "one"
                title = "two"
                title = "three"

                // Assert
                var latest = awaitItem()
                while (titleOf(latest) != "three") latest = awaitItem()
                assertEquals(expected = "three", actual = titleOf(latest))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `given empty content, when collected, then the first tree is an empty column`() = runTest {
        // Arrange
        val content: @Composable () -> Unit = {}

        // Act
        val first = liveTree(content).first()

        // Assert
        assertEquals(expected = ColumnNode(children = emptyList()), actual = first)
    }

    private companion object {
        const val EFFECT_DELAY_MS = 1_000L
    }
}
