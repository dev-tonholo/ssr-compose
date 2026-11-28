package dev.tonholo.ssrcompose.producer.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.turbineScope
import dev.tonholo.ssrcompose.producer.ScaledBasicText
import dev.tonholo.ssrcompose.producer.testing.FakeFrameTimeSource
import dev.tonholo.ssrcompose.producer.testing.PlacedBy
import dev.tonholo.ssrcompose.producer.testing.ShownText
import dev.tonholo.ssrcompose.producer.testing.describedBySemanticsNode
import dev.tonholo.ssrcompose.producer.testing.liveScaledTextScene
import dev.tonholo.ssrcompose.producer.testing.liveSetup
import dev.tonholo.ssrcompose.producer.testing.robotoStyle
import dev.tonholo.ssrcompose.producer.testing.sentText
import dev.tonholo.ssrcompose.producer.testing.shown
import dev.tonholo.ssrcompose.producer.testing.textX
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class LiveScreenWakeTest {
    @Test
    fun `given two screens, when a plain write changes a state only one reads, then the other renders no frame`() =
        runTest {
            // Arrange
            var text by mutableStateOf(BEFORE)
            val screens = ScreensOnOneSceneThread(scope = this)
            val changed = screens.screen { ShownText(text = text) }
            val unrelated = screens.screen { ShownText(text = UNRELATED) }

            turbineScope {
                val changedFrames = changed.screen.frames().testIn(backgroundScope)
                val unrelatedFrames = unrelated.screen.frames().testIn(backgroundScope)
                changedFrames.awaitItem()
                unrelatedFrames.awaitItem()
                settle()
                val rendersWhenSettled = unrelated.renders

                // Act
                text = AFTER
                val shown = changedFrames.awaitItem().shown().texts
                settle()

                // Assert
                assertEquals(expected = listOf(AFTER), actual = shown)
                assertEquals(expected = 0, actual = unrelated.renders - rendersWhenSettled)
                cancelAll(changedFrames, unrelatedFrames)
            }
        }

    @Test
    fun `given two screens, when one's effect changes a state only it reads, then the other renders no frame`() =
        runTest {
            // Arrange
            val change = CompletableDeferred<Unit>()
            val screens = ScreensOnOneSceneThread(scope = this)
            val changed = screens.screen {
                var text by remember { mutableStateOf(BEFORE) }
                ShownText(text = text)
                LaunchedEffect(Unit) {
                    change.await()
                    text = AFTER
                }
            }
            val unrelated = screens.screen { ShownText(text = UNRELATED) }

            turbineScope {
                val changedFrames = changed.screen.frames().testIn(backgroundScope)
                val unrelatedFrames = unrelated.screen.frames().testIn(backgroundScope)
                changedFrames.awaitItem()
                unrelatedFrames.awaitItem()
                settle()
                val rendersWhenSettled = unrelated.renders

                // Act
                change.complete(Unit)
                val shown = changedFrames.awaitItem().shown().texts
                settle()

                // Assert
                assertEquals(expected = listOf(AFTER), actual = shown)
                assertEquals(expected = 0, actual = unrelated.renders - rendersWhenSettled)
                cancelAll(changedFrames, unrelatedFrames)
            }
        }

    @Test
    fun `given a position only placing reads, when another screen's effect writes it in a snapshot, then it shows`() =
        runTest {
            // Arrange
            val shift = mutableIntStateOf(0)
            val change = CompletableDeferred<Unit>()
            val screens = ScreensOnOneSceneThread(scope = this)
            val moved = screens.screen {
                PlacedBy(content = { ShownText(text = BEFORE) }) { placeable ->
                    placeable.place(x = shift.intValue, y = 0)
                }
            }
            val writer = screens.screen { WritesInASnapshot(after = change) { shift.intValue = SHIFT_PX } }

            turbineScope {
                val movedFrames = moved.screen.frames().testIn(backgroundScope)
                val writerFrames = writer.screen.frames().testIn(backgroundScope)
                val before = movedFrames.awaitItem().textX()
                writerFrames.awaitItem()
                settle()

                // Act
                change.complete(Unit)
                settle()
                val after = movedFrames.expectMostRecentItem().textX()

                // Assert
                assertEquals(expected = listOf(0f, SHIFT_PX.toFloat()), actual = listOf(before, after))
                cancelAll(movedFrames, writerFrames)
            }
        }

    @Test
    fun `given a position only a layer reads, when another screen's effect writes it in a snapshot, then it shows`() =
        runTest {
            // Arrange
            val shift = mutableIntStateOf(0)
            val change = CompletableDeferred<Unit>()
            val screens = ScreensOnOneSceneThread(scope = this)
            val moved = screens.screen {
                PlacedBy(content = { ShownText(text = BEFORE) }) { placeable ->
                    placeable.placeWithLayer(x = 0, y = 0) { translationX = shift.intValue.toFloat() }
                }
            }
            val writer = screens.screen { WritesInASnapshot(after = change) { shift.intValue = SHIFT_PX } }

            turbineScope {
                val movedFrames = moved.screen.frames().testIn(backgroundScope)
                val writerFrames = writer.screen.frames().testIn(backgroundScope)
                val before = movedFrames.awaitItem().textX()
                writerFrames.awaitItem()
                settle()

                // Act
                change.complete(Unit)
                settle()
                val after = movedFrames.expectMostRecentItem().textX()

                // Assert
                assertEquals(expected = listOf(0f, SHIFT_PX.toFloat()), actual = listOf(before, after))
                cancelAll(movedFrames, writerFrames)
            }
        }

    @Test
    fun `given a frame that changes a state only another screen reads, when it ends, then no second frame runs`() =
        runTest {
            // Arrange
            var shift by mutableIntStateOf(0)
            var readElsewhere by mutableIntStateOf(0)
            val screens = ScreensOnOneSceneThread(scope = this)
            val changed = screens.screen {
                PlacedBy(content = { ShownText(text = BEFORE) }) { placeable ->
                    val x = shift
                    if (x != 0) readElsewhere = x
                    placeable.place(x = x, y = 0)
                }
            }
            val other = screens.screen { ShownText(text = "$UNRELATED $readElsewhere") }

            turbineScope {
                val changedFrames = changed.screen.frames().testIn(backgroundScope)
                val otherFrames = other.screen.frames().testIn(backgroundScope)
                changedFrames.awaitItem()
                otherFrames.awaitItem()
                settle()
                val rendersWhenSettled = changed.renders

                // Act
                shift = SHIFT_PX
                val movedTo = changedFrames.awaitItem().textX()
                val shownElsewhere = otherFrames.awaitItem().shown().texts
                settle()

                // Assert
                assertEquals(expected = SHIFT_PX.toFloat(), actual = movedTo)
                assertEquals(expected = listOf("$UNRELATED $SHIFT_PX"), actual = shownElsewhere)
                assertEquals(expected = 1, actual = changed.renders - rendersWhenSettled)
                cancelAll(changedFrames, otherFrames)
            }
        }

    @Test
    fun `given a description only a semantics node reads, when its own effect writes it plainly, then it shows`() =
        runTest {
            // Arrange
            val description = mutableStateOf(BEFORE)
            val change = CompletableDeferred<Unit>()
            val screens = ScreensOnOneSceneThread(scope = this)
            val described = screens.screen {
                DescribedText { description.value }
                LaunchedEffect(Unit) {
                    change.await()
                    description.value = AFTER
                }
            }

            turbineScope {
                val frames = described.screen.frames().testIn(backgroundScope)
                val before = frames.awaitItem().description()
                settle()

                // Act
                change.complete(Unit)
                settle()
                val after = frames.expectMostRecentItem().description()

                // Assert
                assertEquals(expected = listOf(BEFORE, AFTER), actual = listOf(before, after))
                cancelAll(frames)
            }
        }

    @Test
    fun `given a description only a semantics node reads, when another screen writes it in a snapshot, then it shows`() =
        runTest {
            // Arrange
            val description = mutableStateOf(BEFORE)
            val change = CompletableDeferred<Unit>()
            val screens = ScreensOnOneSceneThread(scope = this)
            val described = screens.screen { DescribedText { description.value } }
            val writer = screens.screen { WritesInASnapshot(after = change) { description.value = AFTER } }

            turbineScope {
                val describedFrames = described.screen.frames().testIn(backgroundScope)
                val writerFrames = writer.screen.frames().testIn(backgroundScope)
                val before = describedFrames.awaitItem().description()
                writerFrames.awaitItem()
                settle()

                // Act
                change.complete(Unit)
                settle()
                val after = describedFrames.expectMostRecentItem().description()

                // Assert
                assertEquals(expected = listOf(BEFORE, AFTER), actual = listOf(before, after))
                cancelAll(describedFrames, writerFrames)
            }
        }

    private class CountedScreen(val screen: LiveScreen, private val clock: FakeFrameTimeSource) {
        val renders: Int get() = clock.reads
    }

    private class ScreensOnOneSceneThread(private val scope: TestScope) {
        private val dispatcher: CoroutineDispatcher = StandardTestDispatcher(scheduler = scope.testScheduler)
        private val notifier =
            SnapshotNotifier(dispatcher = StandardTestDispatcher(scheduler = scope.testScheduler))

        fun screen(content: @Composable () -> Unit): CountedScreen {
            val clock = FakeFrameTimeSource()
            val screen = LiveScreen(
                content = content,
                setup = liveSetup(),
                scaledTextScene = liveScaledTextScene(),
                dispatcher = dispatcher,
                notifier = notifier,
                frameTime = clock,
            )
            return CountedScreen(screen = screen, clock = clock)
        }
    }

    @Composable
    private fun WritesInASnapshot(after: CompletableDeferred<Unit>, write: () -> Unit) {
        ShownText(text = UNRELATED)
        LaunchedEffect(Unit) {
            after.await()
            Snapshot.withMutableSnapshot(block = write)
        }
    }

    @Composable
    private fun DescribedText(description: () -> String) {
        ScaledBasicText(
            text = UNRELATED,
            modifier = Modifier.describedBySemanticsNode(description = description),
            style = robotoStyle(sizeSp = TEXT_SIZE_SP),
        )
    }

    private fun ScreenFrame.description(): String? = sentText().semantics?.description

    private fun TestScope.settle() {
        advanceTimeBy(SETTLE_TIME)
        runCurrent()
    }

    private suspend fun cancelAll(vararg turbines: ReceiveTurbine<ScreenFrame>) {
        turbines.forEach { it.cancel() }
    }

    private companion object {
        const val BEFORE = "before"
        const val AFTER = "after"
        const val UNRELATED = "unrelated"
        const val SHIFT_PX = 50
        const val TEXT_SIZE_SP = 14
        val SETTLE_TIME = 1.seconds
    }
}
