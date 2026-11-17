package dev.tonholo.ssrcompose.producer.internal

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import dev.tonholo.ssrcompose.core.message.ScreenContent
import dev.tonholo.ssrcompose.producer.testing.Label
import dev.tonholo.ssrcompose.producer.testing.ManualDispatcher
import dev.tonholo.ssrcompose.producer.testing.expectedLabel
import dev.tonholo.ssrcompose.producer.testing.expectedScreen
import dev.tonholo.ssrcompose.producer.testing.runUntilIdle
import java.util.concurrent.CountDownLatch
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

@OptIn(ExperimentalCoroutinesApi::class)
class LiveScreenTest {
    @Test
    fun `given content reading state, when the state changes, then a new screen content is emitted`() = runTest {
        // Arrange
        var text by mutableStateOf(FIRST_TEXT)
        val screen = LiveScreen(
            content = { Label(text = text) },
            dispatcher = testDispatcher(),
            notifier = testNotifier(),
        )

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
                notifier = testNotifier(),
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
        val screen = LiveScreen(content = {}, dispatcher = testDispatcher(), notifier = testNotifier())

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
            notifier = testNotifier(),
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
                notifier = testNotifier(),
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

    @Test
    fun `given many idle screens on their own threads, when every state changes at once, then each shows its change`() =
        runTest {
            // Arrange
            val states = List(SCREEN_COUNT) { mutableStateOf(FIRST_TEXT) }
            val shown = List(SCREEN_COUNT) { MutableStateFlow<ScreenContent?>(null) }
            val expected = List(SCREEN_COUNT) { index -> expectedScreen(expectedLabel(text = changedText(index))) }

            val actual = withContext(Dispatchers.Default) {
                withTimeout(CONCURRENT_TIMEOUT) {
                    val collectors = states.mapIndexed { index, state ->
                        launch {
                            LiveScreen(
                                content = { Label(text = state.value) },
                                dispatcher = serialDefaultDispatcher(),
                            ).contents().collect { shown[index].value = it }
                        }
                    }
                    shown.forEach { screen -> screen.first { it != null } }

                    // Act
                    states.mapIndexed { index, state -> launch { state.value = changedText(index) } }.joinAll()
                    val changed = shown.mapIndexed { index, screen -> screen.first { it == expected[index] } }
                    collectors.forEach { it.cancelAndJoin() }
                    changed
                }
            }

            // Assert
            assertEquals(expected = expected, actual = actual)
        }

    @Test
    fun `given a screen whose composition blocks, when another screen changes twice, then it shows both changes`() =
        runTest {
            // Arrange
            val release = CountDownLatch(1)
            val compositionBlocked = CompletableDeferred<Unit>()
            var blockingText by mutableStateOf(FIRST_TEXT)
            var watchedText by mutableStateOf(FIRST_TEXT)
            val blockingScreen = LiveScreen(
                content = {
                    Label(text = blockingText)
                    if (blockingText == SECOND_TEXT) {
                        compositionBlocked.complete(Unit)
                        release.await()
                    }
                },
                dispatcher = serialDefaultDispatcher(),
            )
            val watchedScreen = LiveScreen(
                content = { Label(text = watchedText) },
                dispatcher = serialDefaultDispatcher(),
            )
            val blockingShown = MutableStateFlow<ScreenContent?>(null)
            val watchedShown = MutableStateFlow<ScreenContent?>(null)

            val shown = withContext(Dispatchers.Default) {
                withTimeout(CONCURRENT_TIMEOUT) {
                    val collectors = listOf(
                        launch { blockingScreen.contents().collect { blockingShown.value = it } },
                        launch { watchedScreen.contents().collect { watchedShown.value = it } },
                    )
                    try {
                        blockingShown.first { it != null }
                        watchedShown.first { it != null }

                        // Act
                        blockingText = SECOND_TEXT
                        compositionBlocked.await()
                        watchedText = SECOND_TEXT
                        val afterFirstChange = watchedShown.first { it == expectedScreen(expectedLabel(SECOND_TEXT)) }
                        watchedText = THIRD_TEXT
                        val afterSecondChange = watchedShown.first { it == expectedScreen(expectedLabel(THIRD_TEXT)) }
                        listOf(afterFirstChange, afterSecondChange)
                    } finally {
                        release.countDown()
                        collectors.forEach { it.cancelAndJoin() }
                    }
                }
            }

            // Assert
            val expected = listOf(
                expectedScreen(expectedLabel(text = SECOND_TEXT)),
                expectedScreen(expectedLabel(text = THIRD_TEXT)),
            )
            assertEquals(expected = expected, actual = shown)
        }

    @Test
    fun `given a screen whose dispatcher is blocked, when it is disposed as another changes, then that one shows it`() =
        runTest {
            // Arrange
            var text by mutableStateOf(FIRST_TEXT)
            var shown: ScreenContent? = null
            val blockedDispatcher = ManualDispatcher()
            val watchedDispatcher = ManualDispatcher()
            val notifierDispatcher = ManualDispatcher()
            val notifier = SnapshotNotifier(dispatcher = notifierDispatcher)
            val blockedScreen = LiveScreen(
                content = { Label(text = FIRST_TEXT) },
                dispatcher = blockedDispatcher,
                notifier = notifier,
            )
            val watchedScreen = LiveScreen(
                content = { Label(text = text) },
                dispatcher = watchedDispatcher,
                notifier = notifier,
            )
            val blockedCollector = backgroundScope.launch(Dispatchers.Unconfined) {
                blockedScreen.contents().collect {}
            }
            val watchedCollector = backgroundScope.launch(Dispatchers.Unconfined) {
                watchedScreen.contents().collect { shown = it }
            }
            runUntilIdle(blockedDispatcher, watchedDispatcher, notifierDispatcher)

            // Act
            text = SECOND_TEXT
            blockedCollector.cancel()
            runUntilIdle(watchedDispatcher, notifierDispatcher)

            // Assert
            assertEquals(expected = expectedScreen(expectedLabel(text = SECOND_TEXT)), actual = shown)
            watchedCollector.cancel()
            runUntilIdle(blockedDispatcher, watchedDispatcher, notifierDispatcher)
        }

    @Test
    fun `given two screens, when one is disposed as a change arrives, then the other still shows the change`() =
        runTest {
            // Arrange
            var text by mutableStateOf(FIRST_TEXT)
            val dispatcher = testDispatcher()
            val notifier = testNotifier()
            turbineScope {
                val disposedScreen = LiveScreen(
                    content = { Label(text = FIRST_TEXT) },
                    dispatcher = dispatcher,
                    notifier = notifier,
                ).contents().testIn(backgroundScope)
                val keptScreen = LiveScreen(
                    content = { Label(text = text) },
                    dispatcher = dispatcher,
                    notifier = notifier,
                ).contents().testIn(backgroundScope)
                disposedScreen.awaitItem()
                keptScreen.awaitItem()

                // Act
                text = SECOND_TEXT
                disposedScreen.cancel()

                val updated = keptScreen.awaitItem()
                text = THIRD_TEXT
                val updatedAgain = keptScreen.awaitItem()

                // Assert
                assertEquals(expected = expectedScreen(expectedLabel(text = SECOND_TEXT)), actual = updated)
                assertEquals(expected = expectedScreen(expectedLabel(text = THIRD_TEXT)), actual = updatedAgain)
                keptScreen.cancel()
            }
        }

    private fun serialDefaultDispatcher() = Dispatchers.Default.limitedParallelism(parallelism = 1)

    private fun changedText(index: Int) = "$SECOND_TEXT $index"

    private fun TestScope.testDispatcher() = StandardTestDispatcher(scheduler = testScheduler)

    private fun TestScope.testNotifier() = SnapshotNotifier(dispatcher = testDispatcher())

    private companion object {
        const val FIRST_TEXT = "Welcome"
        const val SECOND_TEXT = "Updated"
        const val COMPOSITIONS_AFTER_REBUILD = 2
        const val THIRD_TEXT = "Updated again"
        const val SCREEN_COUNT = 100
        val CONCURRENT_TIMEOUT = 10.seconds
    }
}
