package dev.tonholo.ssrcompose.producer.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import app.cash.turbine.test
import dev.tonholo.ssrcompose.producer.testing.FakeFrameTimeSource
import dev.tonholo.ssrcompose.producer.testing.ShownFrame
import dev.tonholo.ssrcompose.producer.testing.ShownText
import dev.tonholo.ssrcompose.producer.testing.liveScaledTextScene
import dev.tonholo.ssrcompose.producer.testing.liveSetup
import dev.tonholo.ssrcompose.producer.testing.shown
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class LiveScreenFrameTest {
    @Test
    fun `given an effect that waits and then asks for a frame, when it changes state after, then the change shows`() =
        runTest {
            // Arrange
            var text by mutableStateOf(FIRST_TEXT)
            val screen = testScreen {
                ShownText(text = text)
                LaunchedEffect(Unit) {
                    delay(EFFECT_DELAY)
                    withFrameNanos {}
                    text = SECOND_TEXT
                }
            }

            screen.frames().test {
                // Act
                val first = awaitItem().shown()
                val second = awaitItem().shown()

                // Assert
                assertEquals(expected = listOf(FIRST_TEXT), actual = first.texts)
                assertEquals(expected = ShownFrame(revision = 2, texts = listOf(SECOND_TEXT)), actual = second)
            }
        }

    @Test
    fun `given an effect that reads frame times, when the frames render, then each time is later than the last`() =
        runTest {
            // Arrange
            val times = mutableListOf<Long>()
            val done = CompletableDeferred<Unit>()
            val screen = testScreen {
                ShownText(text = FIRST_TEXT)
                LaunchedEffect(Unit) {
                    repeat(FRAMES) { withFrameNanos { times += it } }
                    done.complete(Unit)
                }
            }

            screen.frames().test {
                awaitItem()

                // Act
                done.await()

                // Assert
                val steps = times.zipWithNext { earlier, later -> later - earlier }
                assertEquals(expected = FRAMES, actual = times.size)
                assertTrue(actual = times.all { it > 0 })
                assertTrue(actual = steps.all { it > 0 })
            }
        }

    @Test
    fun `given an idle screen, when time passes, then it renders no more frames`() = runTest {
        // Arrange
        val clock = FakeFrameTimeSource()
        val screen = testScreen(frameTime = clock) { ShownText(text = FIRST_TEXT) }

        screen.frames().test {
            awaitItem()
            advanceTimeBy(SETTLE_TIME)
            val readsWhenSettled = clock.reads

            // Act
            advanceTimeBy(IDLE_TIME)

            // Assert
            assertEquals(expected = readsWhenSettled, actual = clock.reads)
        }
    }

    @Test
    fun `given a screen that always asks for a frame, when a second passes, then it renders one frame per 16 ms`() =
        runTest {
            // Arrange
            val clock = FakeFrameTimeSource()
            val screen = testScreen(frameTime = clock) {
                ShownText(text = FIRST_TEXT)
                LaunchedEffect(Unit) { while (true) withFrameNanos {} }
            }

            screen.frames().test {
                awaitItem()
                advanceTimeBy(SETTLE_TIME)
                val readsWhenRunning = clock.reads

                // Act
                advanceTimeBy(ANIMATION_TIME)

                // Assert
                val frames = clock.reads - readsWhenRunning
                assertTrue(actual = frames in EXPECTED_FRAMES..EXPECTED_FRAMES + 1, message = "rendered $frames")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `given an effect that throws while another keeps asking for frames, when collected, then the flow fails`() =
        runTest {
            // Arrange
            val screen = testScreen {
                ShownText(text = FIRST_TEXT)
                LaunchedEffect(Unit) { while (true) withFrameNanos {} }
                LaunchedEffect(Unit) {
                    delay(EFFECT_DELAY)
                    error(EFFECT_ERROR)
                }
            }

            screen.frames().test {
                // Act
                awaitItem()
                val failure = awaitError()

                // Assert
                assertEquals(expected = EFFECT_ERROR, actual = failure.message)
            }
        }

    private fun TestScope.testScreen(
        frameTime: FakeFrameTimeSource = FakeFrameTimeSource(),
        content: @Composable () -> Unit,
    ) = LiveScreen(
        content = content,
        setup = liveSetup(),
        scaledTextScene = liveScaledTextScene(),
        dispatcher = StandardTestDispatcher(scheduler = testScheduler),
        notifier = SnapshotNotifier(dispatcher = StandardTestDispatcher(scheduler = testScheduler)),
        frameTime = frameTime,
    )

    private companion object {
        const val FIRST_TEXT = "Welcome"
        const val SECOND_TEXT = "Updated"
        const val EFFECT_ERROR = "the effect failed"
        const val FRAMES = 3
        val EFFECT_DELAY = 20.milliseconds
        val SETTLE_TIME = 1.seconds
        val IDLE_TIME = 10.seconds
        val ANIMATION_TIME = 1.seconds
        val FRAME_TIME = 16.milliseconds
        val EXPECTED_FRAMES = (ANIMATION_TIME / FRAME_TIME).toInt()
    }
}
