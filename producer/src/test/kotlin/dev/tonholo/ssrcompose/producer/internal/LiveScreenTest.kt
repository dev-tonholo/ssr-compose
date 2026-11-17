package dev.tonholo.ssrcompose.producer.internal

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.cash.turbine.test
import dev.tonholo.ssrcompose.producer.testing.Label
import dev.tonholo.ssrcompose.producer.testing.expectedLabel
import dev.tonholo.ssrcompose.producer.testing.expectedScreen
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class LiveScreenTest {
    @Test
    fun `given content reading state, when the state changes, then a new screen content is emitted`() = runTest {
        // Arrange
        var text by mutableStateOf(FIRST_TEXT)
        val screen = LiveScreen(content = { Label(text = text) }, dispatcher = testDispatcher())

        screen.contents().test {
            // Act
            val first = awaitItem()
            text = SECOND_TEXT
            val second = awaitItem()

            // Assert
            assertEquals(expected = expectedScreen(expectedLabel(text = FIRST_TEXT)), actual = first)
            assertEquals(expected = expectedScreen(expectedLabel(text = SECOND_TEXT)), actual = second)
        }
    }

    @Test
    fun `given content with a component inside another, when collected, then the inner one is in the slot`() =
        runTest {
            // Arrange
            val screen = LiveScreen(
                content = { Label(text = FIRST_TEXT) { Label(text = SECOND_TEXT) } },
                dispatcher = testDispatcher(),
            )

            screen.contents().test {
                // Act
                val content = awaitItem()

                // Assert
                val expected = expectedScreen(
                    expectedLabel(text = FIRST_TEXT, children = listOf(expectedLabel(text = SECOND_TEXT))),
                )
                assertEquals(expected = expected, actual = content)
            }
        }

    @Test
    fun `given content that places nothing, when collected, then an empty screen is emitted`() = runTest {
        // Arrange
        val screen = LiveScreen(content = {}, dispatcher = testDispatcher())

        screen.contents().test {
            // Act
            val content = awaitItem()

            // Assert
            assertEquals(expected = expectedScreen(), actual = content)
        }
    }

    @Test
    fun `given a collector, when it cancels, then the composition is disposed`() = runTest {
        // Arrange
        var disposed = false
        val screen = LiveScreen(
            content = {
                DisposableEffect(Unit) { onDispose { disposed = true } }
                Label(text = FIRST_TEXT)
            },
            dispatcher = testDispatcher(),
        )

        screen.contents().test {
            awaitItem()
            assertFalse(actual = disposed)

            // Act
            cancelAndIgnoreRemainingEvents()
        }
        advanceUntilIdle()

        // Assert
        assertTrue(actual = disposed)
    }

    @Test
    fun `given recomposition that rebuilds the same content, when it is applied, then it is not sent again`() =
        runTest {
            // Arrange
            var generation by mutableIntStateOf(0)
            var compositions = 0
            val screen = LiveScreen(
                content = {
                    SideEffect { compositions++ }
                    key(generation) { Label(text = FIRST_TEXT) }
                },
                dispatcher = testDispatcher(),
            )

            screen.contents().test {
                awaitItem()

                // Act
                generation++
                advanceUntilIdle()

                // Assert
                assertEquals(expected = COMPOSITIONS_AFTER_REBUILD, actual = compositions)
                expectNoEvents()
            }
        }

    private fun TestScope.testDispatcher() = StandardTestDispatcher(scheduler = testScheduler)

    private companion object {
        const val FIRST_TEXT = "Welcome"
        const val SECOND_TEXT = "Updated"
        const val COMPOSITIONS_AFTER_REBUILD = 2
    }
}
