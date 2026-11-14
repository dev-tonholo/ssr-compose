package dev.tonholo.ssrcompose.server.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import app.cash.turbine.test
import dev.tonholo.ssrcompose.protocol.ActionId
import dev.tonholo.ssrcompose.protocol.ColumnNode
import dev.tonholo.ssrcompose.protocol.CounterCard
import dev.tonholo.ssrcompose.protocol.CounterCardProps
import dev.tonholo.ssrcompose.protocol.FragmentNode
import dev.tonholo.ssrcompose.protocol.GreetingCard
import dev.tonholo.ssrcompose.protocol.ReferenceNode
import dev.tonholo.ssrcompose.protocol.ScreenNode
import dev.tonholo.ssrcompose.server.action.ActionRegistry
import dev.tonholo.ssrcompose.server.action.LocalActionRegistry
import dev.tonholo.ssrcompose.server.tree.liveTree
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class CounterScreenTest {
    private val registry = ActionRegistry()
    private val clock = MutableClock(current = Instant.parse("2026-01-01T09:00:00Z"))

    private val content: @Composable () -> Unit = {
        CompositionLocalProvider(LocalActionRegistry provides registry) { CounterScreen(clock = clock) }
    }

    private fun children(tree: ScreenNode): List<ScreenNode> = assertIs<ColumnNode>(tree).children

    private fun counterProps(tree: ScreenNode): CounterCardProps {
        val reference = assertIs<ReferenceNode>(children(tree)[1])
        return Json.decodeFromJsonElement(CounterCard.propsSerializer, reference.props)
    }

    private fun timeMessage(tree: ScreenNode): String {
        val reference = assertIs<ReferenceNode>(children(tree)[0])
        return Json.decodeFromJsonElement(GreetingCard.propsSerializer, reference.props).message
    }

    private fun fragment(tree: ScreenNode): FragmentNode = assertIs<FragmentNode>(children(tree)[2])

    @Test
    fun `given the counter screen, when first rendered, then it holds both cards and a fragment`() = runTest {
        // Arrange
        val screen = liveTree(content)

        screen.test {
            // Act
            val tree = awaitItem()

            // Assert
            assertEquals(expected = 3, actual = children(tree).size)
            assertEquals(expected = "09:00:00", actual = timeMessage(tree))
            assertEquals(expected = 0, actual = counterProps(tree).count)
            assertTrue(fragment(tree).document.isNotEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given the counter screen, when add one is dispatched, then the card shows 1 and the fragment changes`() =
        runTest {
            // Arrange
            liveTree(content).test {
                val before = awaitItem()

                // Act
                registry.dispatch(counterProps(before).onAddOne)
                val after = awaitItem()

                // Assert
                assertEquals(expected = 1, actual = counterProps(after).count)
                assertFalse(fragment(before).document.contentEquals(fragment(after).document))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `given the counter screen, when the banner action is dispatched, then the count goes up too`() = runTest {
        // Arrange
        liveTree(content).test {
            awaitItem()

            // Act
            registry.dispatch(ActionId(BANNER_ACTION_FOR_TEST))
            val after = awaitItem()

            // Assert
            assertEquals(expected = 1, actual = counterProps(after).count)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given the counter screen, when a second passes, then the time changes and the fragment stays`() = runTest {
        // Arrange
        liveTree(content).test {
            val before = awaitItem()
            clock.current = clock.current.plusSeconds(1)

            // Act
            val after = awaitItem()

            // Assert
            assertEquals(expected = "09:00:01", actual = timeMessage(after))
            assertSame(expected = fragment(before).document, actual = fragment(after).document)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private companion object {
        const val BANNER_ACTION_FOR_TEST = "action-2"
    }
}
