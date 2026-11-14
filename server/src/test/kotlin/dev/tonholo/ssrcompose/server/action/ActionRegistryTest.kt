package dev.tonholo.ssrcompose.server.action

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.cash.turbine.test
import dev.tonholo.ssrcompose.protocol.ActionId
import dev.tonholo.ssrcompose.protocol.GreetingCard
import dev.tonholo.ssrcompose.protocol.GreetingCardProps
import dev.tonholo.ssrcompose.server.tree.Reference
import dev.tonholo.ssrcompose.server.tree.liveTree
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class ActionRegistryTest {
    private fun marker(value: Int) = GreetingCardProps(title = "t", message = value.toString())

    @Test
    fun `given a remembered action, when its id is dispatched, then the latest lambda runs`() = runTest {
        // Arrange
        val registry = ActionRegistry()
        val calls = mutableListOf<Int>()
        var version by mutableStateOf(1)
        var id: ActionId? = null
        val content: @Composable () -> Unit = {
            CompositionLocalProvider(LocalActionRegistry provides registry) {
                val current = version
                id = rememberAction { calls += current }
                Reference(GreetingCard, marker(current))
            }
        }

        liveTree(content).test {
            awaitItem()
            val firstId = id
            version = 2
            awaitItem()

            // Act
            val handled = registry.dispatch(id = checkNotNull(firstId))

            // Assert
            assertTrue(handled)
            assertEquals(expected = firstId, actual = id)
            assertEquals(expected = listOf(2), actual = calls)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given an unknown action id, when dispatched, then it returns false`() {
        // Arrange
        val registry = ActionRegistry()

        // Act
        val handled = registry.dispatch(id = ActionId("missing"))

        // Assert
        assertFalse(handled)
    }

    @Test
    fun `given an action that leaves the composition, when its id is dispatched, then it returns false`() =
        runTest {
            // Arrange
            val registry = ActionRegistry()
            var visible by mutableStateOf(true)
            var id: ActionId? = null
            val content: @Composable () -> Unit = {
                CompositionLocalProvider(LocalActionRegistry provides registry) {
                    if (visible) id = rememberAction {}
                    Reference(GreetingCard, marker(if (visible) 1 else 0))
                }
            }

            liveTree(content).test {
                awaitItem()
                val removedId = checkNotNull(id)
                visible = false
                awaitItem()

                // Act
                val handled = registry.dispatch(id = removedId)

                // Assert
                assertFalse(handled)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
