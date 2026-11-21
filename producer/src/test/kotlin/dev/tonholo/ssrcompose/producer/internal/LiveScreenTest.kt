package dev.tonholo.ssrcompose.producer.internal

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import dev.tonholo.ssrcompose.core.document.DocumentNode
import dev.tonholo.ssrcompose.core.document.ScreenDocument
import dev.tonholo.ssrcompose.producer.ScaledBasicText
import dev.tonholo.ssrcompose.producer.internal.reader.NotSendableReason
import dev.tonholo.ssrcompose.producer.internal.scene.SceneSetup
import dev.tonholo.ssrcompose.producer.internal.text.ScaledTextScene
import dev.tonholo.ssrcompose.producer.testing.BAND_CURVE
import dev.tonholo.ssrcompose.producer.testing.BAND_FONT_SCALE
import dev.tonholo.ssrcompose.producer.testing.DEFAULT_SCENE_HEIGHT_PX
import dev.tonholo.ssrcompose.producer.testing.DEFAULT_SCENE_WIDTH_PX
import dev.tonholo.ssrcompose.producer.testing.EMULATOR_CURVE
import dev.tonholo.ssrcompose.producer.testing.EMULATOR_FONT_SCALE
import dev.tonholo.ssrcompose.producer.testing.LINE_HEIGHT_SP
import dev.tonholo.ssrcompose.producer.testing.LIVE_FONT_SCALE
import dev.tonholo.ssrcompose.producer.testing.ManualDispatcher
import dev.tonholo.ssrcompose.producer.testing.SMALL_SIZE_SP
import dev.tonholo.ssrcompose.producer.testing.ShownFrame
import dev.tonholo.ssrcompose.producer.testing.ShownText
import dev.tonholo.ssrcompose.producer.testing.TEST_DENSITY
import dev.tonholo.ssrcompose.producer.testing.TWO_LINES_TEXT
import dev.tonholo.ssrcompose.producer.testing.firstText
import dev.tonholo.ssrcompose.producer.testing.heightOfBasicText
import dev.tonholo.ssrcompose.producer.testing.liveScaledTextScene
import dev.tonholo.ssrcompose.producer.testing.liveSetup
import dev.tonholo.ssrcompose.producer.testing.robotoStyle
import dev.tonholo.ssrcompose.producer.testing.runUntilIdle
import dev.tonholo.ssrcompose.producer.testing.shown
import dev.tonholo.ssrcompose.producer.testing.withoutIds
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield

@OptIn(ExperimentalCoroutinesApi::class)
class LiveScreenTest {
    @Test
    fun `given content reading state, when the state changes, then a new revision shows the change`() = runTest {
        // Arrange
        var text by mutableStateOf(FIRST_TEXT)
        val screen = testScreen { ShownText(text = text) }

        screen.frames().test {
            // Act
            val first = awaitItem().shown()
            text = SECOND_TEXT
            val second = awaitItem().shown()

            // Assert
            assertEquals(expected = ShownFrame(revision = 1, texts = listOf(FIRST_TEXT)), actual = first)
            assertEquals(expected = ShownFrame(revision = 2, texts = listOf(SECOND_TEXT)), actual = second)
        }
    }

    @Test
    fun `given content that shows nothing, when collected, then the root box has the scene's size`() = runTest {
        // Arrange
        val expected = ScreenFrame.Sent(
            revision = 1,
            document = ScreenDocument(
                root = DocumentNode.Box(
                    id = 0,
                    x = 0f,
                    y = 0f,
                    width = DEFAULT_SCENE_WIDTH_PX.toFloat(),
                    height = DEFAULT_SCENE_HEIGHT_PX.toFloat(),
                ),
            ),
        )
        val screen = testScreen {}

        screen.frames().test {
            // Act
            val frame = assertIs<ScreenFrame.Sent>(value = awaitItem())

            // Assert
            assertEquals(expected = expected.document.root, actual = frame.document.root.withoutIds())
            assertEquals(expected = expected.revision, actual = frame.revision)
        }
    }

    @Test
    fun `given content with an image that goes away, when collected, then it is not sendable and then sent`() =
        runTest {
            // Arrange
            var showImage by mutableStateOf(true)
            val screen = testScreen {
                if (showImage) {
                    Image(painter = ColorPainter(Color.Red), contentDescription = null, modifier = Modifier.size(1.dp))
                } else {
                    ShownText(text = FIRST_TEXT)
                }
            }

            screen.frames().test {
                // Act
                val notSendable = awaitItem()
                showImage = false
                val sent = awaitItem().shown()

                // Assert
                assertEquals(expected = ScreenFrame.NotSendable(reason = NotSendableReason.Image), actual = notSendable)
                assertEquals(expected = ShownFrame(revision = 1, texts = listOf(FIRST_TEXT)), actual = sent)
            }
        }

    @Test
    fun `given two screens sharing revisions, when each sends, then the revisions rise across both`() = runTest {
        // Arrange
        val revisions = Revisions()
        val first = testScreen(revisions = revisions) { ShownText(text = FIRST_TEXT) }
        val second = testScreen(revisions = revisions) { ShownText(text = SECOND_TEXT) }

        // Act
        val firstRevision = first.frames().first().shown().revision
        val secondRevision = second.frames().first().shown().revision

        // Assert
        assertEquals(expected = listOf(1L, 2L), actual = listOf(firstRevision, secondRevision))
    }

    @Test
    fun `given a right to left setup, when two boxes sit in a row, then the first is at the right edge`() = runTest {
        // Arrange
        val expectedFirstX = DEFAULT_SCENE_WIDTH_PX - BOX_SIZE_DP.toFloat()
        val screen = testScreen(setup = liveSetup(layoutDirection = LayoutDirection.Rtl)) {
            Row {
                Box(Modifier.size(BOX_SIZE_DP.dp).background(Color.Red))
                Box(Modifier.size(BOX_SIZE_DP.dp).background(Color.Blue))
            }
        }

        // Act
        val frame = screen.frames().first() as ScreenFrame.Sent

        // Assert
        assertEquals(expected = expectedFirstX, actual = frame.document.root.children.first().x)
    }

    @Test
    fun `given a collector, when it cancels, then the composition is disposed`() = runTest {
        // Arrange
        var disposed = false
        val screen = testScreen {
            DisposableEffect(Unit) { onDispose { disposed = true } }
            ShownText(text = FIRST_TEXT)
        }

        screen.frames().test {
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
    fun `given a recomposition that shows the same content, when it is applied, then it is not sent again`() =
        runTest {
            // Arrange
            var generation by mutableIntStateOf(0)
            var compositions = 0
            val screen = testScreen {
                SideEffect { compositions++ }
                ShownText(text = if (generation >= 0) FIRST_TEXT else SECOND_TEXT)
            }

            screen.frames().test {
                awaitItem()

                // Act
                generation++
                advanceUntilIdle()

                // Assert
                assertEquals(expected = COMPOSITIONS_AFTER_RECOMPOSING, actual = compositions)
                expectNoEvents()
            }
        }

    @Test
    fun `given a screen whose dispatcher is blocked, when it is disposed as another changes, then that one shows it`() =
        runTest {
            // Arrange
            var text by mutableStateOf(FIRST_TEXT)
            var shown: List<String>? = null
            val blockedDispatcher = ManualDispatcher()
            val watchedDispatcher = ManualDispatcher()
            val notifierDispatcher = ManualDispatcher()
            val notifier = SnapshotNotifier(dispatcher = notifierDispatcher)
            val blockedScreen = LiveScreen(
                scaledTextScene = liveScaledTextScene(),
                content = { ShownText(text = FIRST_TEXT) },
                setup = liveSetup(),
                dispatcher = blockedDispatcher,
                notifier = notifier,
                framePause = { yield() },
            )
            val watchedScreen = LiveScreen(
                scaledTextScene = liveScaledTextScene(),
                content = { ShownText(text = text) },
                setup = liveSetup(),
                dispatcher = watchedDispatcher,
                notifier = notifier,
                framePause = { yield() },
            )
            val blockedCollector = backgroundScope.launch(Dispatchers.Unconfined) {
                blockedScreen.frames().collect {}
            }
            val watchedCollector = backgroundScope.launch(Dispatchers.Unconfined) {
                watchedScreen.frames().collect { shown = it.shown().texts }
            }
            runUntilIdle(blockedDispatcher, watchedDispatcher, notifierDispatcher)

            // Act
            text = SECOND_TEXT
            blockedCollector.cancel()
            runUntilIdle(watchedDispatcher, notifierDispatcher)

            // Assert
            assertEquals(expected = listOf(SECOND_TEXT), actual = shown)
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
                    scaledTextScene = liveScaledTextScene(),
                    content = { ShownText(text = FIRST_TEXT) },
                    setup = liveSetup(),
                    dispatcher = dispatcher,
                    notifier = notifier,
                ).frames().testIn(backgroundScope)
                val keptScreen = LiveScreen(
                    scaledTextScene = liveScaledTextScene(),
                    content = { ShownText(text = text) },
                    setup = liveSetup(),
                    dispatcher = dispatcher,
                    notifier = notifier,
                ).frames().testIn(backgroundScope)
                disposedScreen.awaitItem()
                keptScreen.awaitItem()

                // Act
                text = SECOND_TEXT
                disposedScreen.cancel()

                val updated = keptScreen.awaitItem().shown().texts
                text = THIRD_TEXT
                val updatedAgain = keptScreen.awaitItem().shown().texts

                // Assert
                assertEquals(expected = listOf(SECOND_TEXT), actual = updated)
                assertEquals(expected = listOf(THIRD_TEXT), actual = updatedAgain)
                keptScreen.cancel()
            }
        }

    @Test
    fun `given a first composition that throws, when a state is written after, then its thread gets no work`() =
        runTest {
            // Arrange
            val written = mutableIntStateOf(0)
            val sceneDispatcher = ManualDispatcher()
            val notifierDispatcher = ManualDispatcher()
            val screen = LiveScreen(
                scaledTextScene = liveScaledTextScene(),
                content = { error(COMPOSITION_ERROR) },
                setup = liveSetup(),
                dispatcher = sceneDispatcher,
                notifier = SnapshotNotifier(dispatcher = notifierDispatcher),
            )
            val collector = backgroundScope.async(Dispatchers.Unconfined) {
                runCatching { screen.frames().collect {} }.exceptionOrNull()
            }
            runUntilIdle(sceneDispatcher, notifierDispatcher)

            // Act
            written.intValue++
            Snapshot.sendApplyNotifications()

            // Assert
            assertEquals(expected = COMPOSITION_ERROR, actual = collector.await()?.message)
            assertFalse(actual = sceneDispatcher.hasTasks)
        }

    @Test
    fun `given an effect that throws, when collected, then the flow fails with its error`() = runTest {
        // Arrange
        val screen = testScreen {
            ShownText(text = FIRST_TEXT)
            LaunchedEffect(Unit) {
                yield()
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

    @Test
    fun `given a screen resumed inside the apply notification, when a state changes, then it shows the change`() =
        runTest {
            // Arrange
            var text by mutableStateOf(FIRST_TEXT)
            val screen = LiveScreen(
                scaledTextScene = liveScaledTextScene(),
                content = { ShownText(text = text) },
                setup = liveSetup(),
                dispatcher = Dispatchers.Unconfined,
                notifier = testNotifier(),
            )

            screen.frames().test {
                awaitItem()

                // Act
                text = SECOND_TEXT
                val shown = awaitItem().shown().texts

                // Assert
                assertEquals(expected = listOf(SECOND_TEXT), actual = shown)
            }
        }

    @Test
    fun `given a device that moves into the band, when it changes, then each line is curve of 24 dp high`() = runTest {
        // Arrange
        var device by mutableStateOf(value = ScaledTextScene(curve = EMULATOR_CURVE, fontScale = EMULATOR_FONT_SCALE))
        val style = robotoStyle(sizeSp = SMALL_SIZE_SP).copy(lineHeight = LINE_HEIGHT_SP.sp)
        val screen = testScreen(setup = denseSetup(), scaledTextScene = { device }) {
            ScaledBasicText(text = TWO_LINES_TEXT, style = style)
        }
        val emulatorSize = EMULATOR_CURVE.spToDp(sp = SMALL_SIZE_SP.toFloat())
        val proportionalLineHeight = LINE_HEIGHT_SP * emulatorSize / SMALL_SIZE_SP
        val expectedBefore = heightOfBasicText(curve = EMULATOR_CURVE, lineHeightSp = proportionalLineHeight)
        val expectedAfter = heightOfBasicText(curve = BAND_CURVE)

        screen.frames().test {
            // Act
            val before = awaitItem().textHeightPx()
            device = ScaledTextScene(curve = BAND_CURVE, fontScale = BAND_FONT_SCALE)
            val after = awaitItem().textHeightPx()

            // Assert
            assertEquals(expected = expectedBefore, actual = before)
            assertEquals(expected = expectedAfter, actual = after)
        }
    }

    private fun ScreenFrame.textHeightPx(): Int {
        assertIs<ScreenFrame.Sent>(this)
        return (document.root.firstText().height * TEST_DENSITY).roundToInt()
    }

    private fun denseSetup(): SceneSetup = SceneSetup(
        widthPx = DEFAULT_SCENE_WIDTH_PX,
        heightPx = DEFAULT_SCENE_HEIGHT_PX,
        density = Density(density = TEST_DENSITY, fontScale = LIVE_FONT_SCALE),
        layoutDirection = LayoutDirection.Ltr,
    )

    private fun TestScope.testScreen(
        setup: SceneSetup = liveSetup(),
        scaledTextScene: () -> ScaledTextScene = liveScaledTextScene(),
        revisions: Revisions = Revisions(),
        content: @Composable () -> Unit,
    ) = LiveScreen(
        scaledTextScene = scaledTextScene,
        content = content,
        setup = setup,
        dispatcher = testDispatcher(),
        notifier = testNotifier(),
        revisions = revisions,
    )

    private fun TestScope.testDispatcher() = StandardTestDispatcher(scheduler = testScheduler)

    private fun TestScope.testNotifier() = SnapshotNotifier(dispatcher = testDispatcher())

    private companion object {
        const val FIRST_TEXT = "Welcome"
        const val SECOND_TEXT = "Updated"
        const val THIRD_TEXT = "Updated again"
        const val COMPOSITIONS_AFTER_RECOMPOSING = 2
        const val BOX_SIZE_DP = 10
        const val COMPOSITION_ERROR = "the first composition failed"
        const val EFFECT_ERROR = "the effect failed"
    }
}
