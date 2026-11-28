package dev.tonholo.ssrcompose.producer.internal.compat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import dev.tonholo.ssrcompose.producer.internal.scene.SceneSetup
import dev.tonholo.ssrcompose.producer.internal.scene.withSceneLock
import dev.tonholo.ssrcompose.producer.testing.ManualDispatcher
import dev.tonholo.ssrcompose.producer.testing.PlacedBy
import dev.tonholo.ssrcompose.producer.testing.ReplacedContent
import dev.tonholo.ssrcompose.producer.testing.ReplacedContentTracker
import dev.tonholo.ssrcompose.producer.testing.runUntilIdle
import dev.tonholo.ssrcompose.producer.testing.skiaClassesAllocatedBy
import kotlin.coroutines.CoroutineContext
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers

private const val WIDTH_PX = 400
private const val HEIGHT_PX = 1000
private const val DENSITY = 2f

private const val ROWS = 50
private const val NAME_WIDTH_DP = 60
private const val VALUE_WIDTH_DP = 40
private const val ROW_HEIGHT_DP = 10
private const val NAME_WIDTH_PX = 120f
private const val VALUE_WIDTH_PX = 80f
private const val ROW_HEIGHT_PX = 20f

private const val FIRST_FRAME_NANOS = 1_000_000_000L
private const val FRAME_STEP_NANOS = 16_000_000L
private const val TIMED_FRAMES = 3
private const val HOSTS = 3
private const val EFFECT_NAME = "scene effects"
private const val SHIFT_PX = 50
private const val TALLER_ROW_HEIGHT_DP = 30
private const val TALLER_ROW_HEIGHT_PX = 60f
private const val FAILURE = "the first composition fails"
private const val FRAMES_TO_SETTLE = 120
private const val REPLACEMENTS = 2000
private const val OLDEST_GENERATION_A_HOST_MAY_HOLD = REPLACEMENTS - 1
private val RASTER_PIXEL_CLASSES = setOf("Surface", "Bitmap")
private const val TEXT_LAYOUT_CLASS = "Paragraph"

private data class PlacedText(val text: String, val bounds: Rect)

private class Lifetime {
    var entered = 0
    var left = 0
    var compositions = 0
}

@Composable
private fun CountedText(lifetime: Lifetime, text: () -> String) {
    DisposableEffect(Unit) {
        lifetime.entered++
        onDispose { lifetime.left++ }
    }
    val shown = text()
    SideEffect { lifetime.compositions++ }
    BasicText(text = shown)
}

@Composable
private fun FixedText(text: String, widthDp: Int, modifier: Modifier = Modifier) {
    BasicText(text = text, modifier = modifier.size(width = widthDp.dp, height = ROW_HEIGHT_DP.dp))
}

@Composable
private fun TextsReadByLayersAndPlacement(content: ReplacedContent) {
    Column {
        FixedText(
            text = "layer",
            widthDp = NAME_WIDTH_DP,
            modifier = Modifier.graphicsLayer { translationX = content.shiftPx.intValue.toFloat() },
        )
        FixedText(
            text = "layer made while placing",
            widthDp = NAME_WIDTH_DP,
            modifier = Modifier.layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(width = placeable.width, height = placeable.height) {
                    placeable.placeWithLayer(x = 0, y = 0) { translationX = content.shiftPx.intValue.toFloat() }
                }
            },
        )
        FixedText(
            text = "placement",
            widthDp = NAME_WIDTH_DP,
            modifier = Modifier.offset { IntOffset(x = content.shiftPx.intValue, y = 0) },
        )
    }
}

private enum class AnchoredSlot { Anchor, Popup }

/** Adds a popup from the measure step, once [isAnchorPlaced] says so. */
@Composable
private fun PopupOnceAnchorIsPlaced(isAnchorPlaced: () -> Boolean, onAnchorPlaced: () -> Unit) {
    SubcomposeLayout { constraints ->
        val addsPopup = isAnchorPlaced()
        val anchor = subcompose(slotId = AnchoredSlot.Anchor) {
            FixedText(
                text = "anchor",
                widthDp = NAME_WIDTH_DP,
                modifier = Modifier.onGloballyPositioned { onAnchorPlaced() },
            )
        }.single().measure(constraints)
        val popup = if (addsPopup) {
            subcompose(slotId = AnchoredSlot.Popup) { Popup { FixedText(text = "popup", widthDp = NAME_WIDTH_DP) } }
                .map { it.measure(constraints) }
        } else {
            emptyList()
        }
        layout(width = anchor.width, height = anchor.height) {
            (popup + anchor).forEach { it.place(x = 0, y = 0) }
        }
    }
}

private class StateMadeInLayout {
    var shift: MutableState<Int>? = null

    fun readOrMake(): Int = (shift ?: mutableStateOf(value = 0).also { shift = it }).value
}

private fun nameAt(left: Float, top: Float = 0f, height: Float = ROW_HEIGHT_PX, text: String = "moving"): PlacedText =
    PlacedText(text = text, bounds = Rect(left = left, top = top, right = left + NAME_WIDTH_PX, bottom = top + height))

/**
 * What every [SceneHost] must do. An implementation's test class extends this one with the function that opens it.
 */
internal abstract class SceneHostConformanceTest(
    private val openHost: (SceneSetup, CoroutineContext, () -> Unit) -> SceneHost,
) {
    private val openHosts = mutableListOf<SceneHost>()

    @AfterTest
    fun closeOpenHosts() {
        openHosts.toList().forEach { it.closeLocked() }
    }

    @Test
    fun `given a screen of 50 rows, when a frame runs, then its 100 texts are read with their bounds`() {
        // Arrange
        val host = open()
        host.setContentLocked {
            Column {
                repeat(ROWS) { row ->
                    Row {
                        FixedText(text = "name $row", widthDp = NAME_WIDTH_DP)
                        FixedText(text = "value $row", widthDp = VALUE_WIDTH_DP)
                    }
                }
            }
        }

        // Act
        host.renderLocked()
        val texts = host.placedTexts()

        // Assert
        val expected = List(ROWS) { row ->
            val top = row * ROW_HEIGHT_PX
            val bottom = top + ROW_HEIGHT_PX
            listOf(
                PlacedText(
                    text = "name $row",
                    bounds = Rect(left = 0f, top = top, right = NAME_WIDTH_PX, bottom = bottom),
                ),
                PlacedText(
                    text = "value $row",
                    bounds = Rect(
                        left = NAME_WIDTH_PX,
                        top = top,
                        right = NAME_WIDTH_PX + VALUE_WIDTH_PX,
                        bottom = bottom,
                    ),
                ),
            )
        }.flatten()
        assertEquals(expected = expected, actual = texts)
    }

    @Test
    fun `given a right to left screen, when content sits at its bottom start, then it lands bottom right`() {
        // Arrange
        val host = open(layoutDirection = LayoutDirection.Rtl)
        host.setContentLocked {
            Box(modifier = Modifier.fillMaxSize()) {
                BasicText(
                    text = "corner",
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(width = NAME_WIDTH_DP.dp, height = ROW_HEIGHT_DP.dp),
                )
            }
        }

        // Act
        host.renderLocked()
        val texts = host.placedTexts()

        // Assert
        val bounds = Rect(
            left = WIDTH_PX - NAME_WIDTH_PX,
            top = HEIGHT_PX - ROW_HEIGHT_PX,
            right = WIDTH_PX.toFloat(),
            bottom = HEIGHT_PX.toFloat(),
        )
        assertEquals(expected = listOf(PlacedText(text = "corner", bounds = bounds)), actual = texts)
    }

    @Test
    fun `given a screen showing a state, when the state changes, then one composition recomposes once in place`() {
        // Arrange
        var label by mutableStateOf(value = "before")
        val lifetime = Lifetime()
        val host = open()
        host.setContentLocked { CountedText(lifetime = lifetime) { label } }
        host.renderLocked()
        val idleBefore = !host.hasPendingWork()
        val idBefore = host.textNodes().single().id

        // Act
        Snapshot.withMutableSnapshot { label = "after" }
        val pendingAfterWrite = host.hasPendingWork()
        host.renderLocked()

        // Assert
        assertEquals(expected = true, actual = idleBefore)
        assertEquals(expected = true, actual = pendingAfterWrite)
        assertEquals(expected = false, actual = host.hasPendingWork())
        assertEquals(expected = listOf("after"), actual = host.placedTexts().map { it.text })
        assertEquals(expected = idBefore, actual = host.textNodes().single().id)
        assertEquals(expected = 2, actual = lifetime.compositions)
        assertEquals(expected = 1, actual = lifetime.entered)
        assertEquals(expected = 0, actual = lifetime.left)
    }

    @Test
    fun `given an effect waiting for frames, when frames run, then it gets the time of each frame`() {
        // Arrange
        val seen = mutableListOf<Long>()
        val host = open()
        host.setContentLocked {
            LaunchedEffect(Unit) { repeat(TIMED_FRAMES) { withFrameNanos { time -> seen += time } } }
        }
        val times = List(TIMED_FRAMES) { index -> FIRST_FRAME_NANOS + index * FRAME_STEP_NANOS }

        // Act
        times.forEach { time -> host.renderLocked(nanoTime = time) }

        // Assert
        assertEquals(expected = times, actual = seen)
    }

    @Test
    fun `given a coroutine context, when an effect resumes, then it runs on that context's dispatcher`() {
        // Arrange
        val dispatcher = ManualDispatcher()
        val signal = CompletableDeferred<Unit>()
        var resumedWith: String? = null
        val host = open(coroutineContext = dispatcher + CoroutineName(name = EFFECT_NAME))
        host.setContentLocked {
            LaunchedEffect(Unit) {
                signal.await()
                resumedWith = coroutineContext[CoroutineName]?.name
            }
        }
        host.renderLocked()
        runUntilIdle(dispatcher)

        // Act
        signal.complete(Unit)
        val beforeDispatcherRuns = resumedWith
        runUntilIdle(dispatcher)

        // Assert
        assertNull(actual = beforeDispatcherRuns)
        assertEquals(expected = EFFECT_NAME, actual = resumedWith)
    }

    @Test
    fun `given several live hosts with pending work, when one runs a frame, then the others compose nothing`() {
        // Arrange
        var tick by mutableStateOf(value = 0)
        val lifetimes = List(HOSTS) { Lifetime() }
        val hosts = lifetimes.map { lifetime ->
            open().also { host ->
                host.setContentLocked { CountedText(lifetime = lifetime) { "tick $tick" } }
                host.renderLocked()
            }
        }
        Snapshot.withMutableSnapshot { tick = 1 }

        // Act
        hosts.first().renderLocked()

        // Assert
        assertEquals(expected = listOf(2, 1, 1), actual = lifetimes.map { it.compositions })
        assertEquals(expected = listOf(false, true, true), actual = hosts.map { it.hasPendingWork() })
        assertEquals(
            expected = listOf("tick 1", "tick 0", "tick 0"),
            actual = hosts.flatMap { host -> host.placedTexts().map { it.text } },
        )
    }

    @Test
    fun `given a host showing content, when it closes, then its composition is disposed`() {
        // Arrange
        val lifetime = Lifetime()
        val host = open()
        host.setContentLocked { CountedText(lifetime = lifetime) { "shown" } }
        host.renderLocked()

        // Act
        host.closeLocked()

        // Assert
        assertEquals(expected = 1, actual = lifetime.left)
    }

    @Test
    fun `given a screen with a popup and a dialog, when it settles, then each window has a root of its own`() {
        // Arrange
        val host = open()
        host.setContentLocked {
            BasicText(text = "screen")
            Popup { BasicText(text = "popup") }
            Dialog(onDismissRequest = {}) { BasicText(text = "dialog") }
        }

        // Act
        host.renderUntilIdle()
        val windows = host.textsByWindow()

        // Assert
        assertEquals(expected = listOf(listOf("dialog"), listOf("popup"), listOf("screen")), actual = windows)
    }

    @Test
    fun `given a screen showing a popup, when the popup leaves, then its root is gone`() {
        // Arrange
        var shown by mutableStateOf(value = true)
        val host = open()
        host.setContentLocked {
            BasicText(text = "screen")
            if (shown) Popup { BasicText(text = "popup") }
        }
        host.renderUntilIdle()

        // Act
        Snapshot.withMutableSnapshot { shown = false }
        host.renderUntilIdle()

        // Assert
        assertEquals(expected = listOf(listOf("screen")), actual = host.textsByWindow())
    }

    @Test
    fun `given a popup that layout adds late in a frame, when the frames settle, then the popup is laid out`() {
        // Arrange
        var anchorPlaced by mutableStateOf(value = false)
        val host = open()
        host.setContentLocked {
            PopupOnceAnchorIsPlaced(isAnchorPlaced = { anchorPlaced }, onAnchorPlaced = { anchorPlaced = true })
        }

        // Act
        host.renderLocked()
        val pendingAfterFirstFrame = host.hasPendingWork()
        host.renderWhilePending()
        val popup = host.placedTexts().single { it.text == "popup" }

        // Assert
        assertEquals(expected = true, actual = pendingAfterFirstFrame)
        assertEquals(expected = Size(width = NAME_WIDTH_PX, height = ROW_HEIGHT_PX), actual = popup.bounds.size)
    }

    @Test
    fun `given a text below another, when an update makes the upper one taller, then both new bounds are read`() {
        // Arrange
        var upperHeightDp by mutableStateOf(value = ROW_HEIGHT_DP)
        val host = open()
        host.setContentLocked {
            Column {
                BasicText(text = "upper", modifier = Modifier.size(width = NAME_WIDTH_DP.dp, height = upperHeightDp.dp))
                FixedText(text = "lower", widthDp = NAME_WIDTH_DP)
            }
        }
        host.renderUntilIdle()

        // Act
        Snapshot.withMutableSnapshot { upperHeightDp = TALLER_ROW_HEIGHT_DP }
        host.renderUntilIdle()

        // Assert
        val expected = listOf(
            nameAt(left = 0f, height = TALLER_ROW_HEIGHT_PX, text = "upper"),
            nameAt(left = 0f, top = TALLER_ROW_HEIGHT_PX, text = "lower"),
        )
        assertEquals(expected = expected, actual = host.placedTexts())
    }

    @Test
    fun `given a position read only while placing, when the state changes, then the new bounds are read`() {
        // Arrange
        var shift by mutableStateOf(value = 0)
        val host = open()
        host.setContentLocked {
            FixedText(text = "moving", widthDp = NAME_WIDTH_DP, modifier = Modifier.offset { IntOffset(shift, 0) })
        }
        host.renderUntilIdle()

        // Act
        Snapshot.withMutableSnapshot { shift = SHIFT_PX }
        host.renderUntilIdle()

        // Assert
        assertEquals(expected = listOf(nameAt(left = SHIFT_PX.toFloat())), actual = host.placedTexts())
    }

    @Test
    fun `given a position read only by a layer, when the state changes, then the new bounds are read`() {
        // Arrange
        var shift by mutableStateOf(value = 0)
        val host = open()
        host.setContentLocked {
            PlacedBy(content = { FixedText(text = "moving", widthDp = NAME_WIDTH_DP) }) { placeable ->
                placeable.placeWithLayer(x = 0, y = 0) { translationX = shift.toFloat() }
            }
        }
        host.renderUntilIdle()

        // Act
        Snapshot.withMutableSnapshot { shift = SHIFT_PX }
        val pendingAfterWrite = host.hasPendingWork()
        host.renderUntilIdle()

        // Assert
        assertEquals(expected = true, actual = pendingAfterWrite)
        assertEquals(expected = listOf(nameAt(left = SHIFT_PX.toFloat())), actual = host.placedTexts())
    }

    @Test
    fun `given a position that follows another node's reported bounds, when one frame runs, then it is settled`() {
        // Arrange
        var reportedWidth by mutableStateOf(value = 0)
        val host = open()
        host.setContentLocked {
            Column {
                FixedText(
                    text = "sizer",
                    widthDp = NAME_WIDTH_DP,
                    modifier = Modifier.onGloballyPositioned { reportedWidth = it.size.width },
                )
                FixedText(
                    text = "follower",
                    widthDp = NAME_WIDTH_DP,
                    modifier = Modifier.offset { IntOffset(reportedWidth, 0) },
                )
            }
        }

        // Act
        host.renderLocked()

        // Assert
        val expected = listOf(
            nameAt(left = 0f, text = "sizer"),
            nameAt(left = NAME_WIDTH_PX, top = ROW_HEIGHT_PX, text = "follower"),
        )
        assertEquals(expected = expected, actual = host.placedTexts())
    }

    @Test
    fun `given a state first made in a frame's last layout pass, when written later, then the new bounds are read`() {
        // Arrange
        var reported by mutableStateOf(value = false)
        val state = StateMadeInLayout()
        val host = open()
        host.setContentLocked {
            PlacedBy(
                content = {
                    FixedText(
                        text = "moving",
                        widthDp = NAME_WIDTH_DP,
                        modifier = Modifier.onGloballyPositioned { reported = true },
                    )
                },
            ) { placeable -> placeable.place(x = if (reported) state.readOrMake() else 0, y = 0) }
        }
        host.renderLocked()

        // Act
        checkNotNull(state.shift).value = SHIFT_PX
        host.renderLocked()
        host.renderUntilIdle()

        // Assert
        assertEquals(expected = listOf(nameAt(left = SHIFT_PX.toFloat())), actual = host.placedTexts())
    }

    @Test
    fun `given content whose first composition throws, when it is set, then setContent throws that error`() {
        // Arrange
        val host = open()

        // Act
        val thrown = assertFailsWith<IllegalStateException> { host.setContentLocked { error(FAILURE) } }

        // Assert
        assertEquals(expected = FAILURE, actual = thrown.message)
    }

    @Test
    fun `given a screen with text, when it opens, composes and renders, then no raster surface is made`() {
        // Arrange
        val content: @Composable () -> Unit = {
            BasicText(text = "screen")
            Popup { BasicText(text = "popup") }
        }

        // Act
        val allocated = skiaClassesAllocatedBy {
            val host = open()
            host.setContentLocked(content = content)
            host.renderUntilIdle()
        }

        // Assert
        assertEquals(expected = true, actual = TEXT_LAYOUT_CLASS in allocated)
        assertEquals(expected = emptySet(), actual = allocated intersect RASTER_PIXEL_CLASSES)
    }

    @Test
    fun `given content with a draw step, when frames run, then it is never drawn`() {
        // Arrange
        var draws = 0
        var heightDp by mutableStateOf(value = ROW_HEIGHT_DP)
        val host = open()
        host.setContentLocked {
            Box(modifier = Modifier.size(width = NAME_WIDTH_DP.dp, height = heightDp.dp).drawBehind { draws++ })
        }
        host.renderUntilIdle()

        // Act
        Snapshot.withMutableSnapshot { heightDp = TALLER_ROW_HEIGHT_DP }
        host.renderUntilIdle()

        // Assert
        assertEquals(expected = 0, actual = draws)
    }

    @Test
    fun `given texts read by layers and placement, when new keys replace them often, then old ones are let go`() {
        // Arrange
        var generation by mutableStateOf(value = 0)
        val tracker = ReplacedContentTracker()
        val host = open()
        host.setContentLocked {
            val current = generation
            key(current) { TextsReadByLayersAndPlacement(content = remember { tracker.make(generation = current) }) }
        }
        host.renderUntilIdle()

        // Act
        repeat(REPLACEMENTS) { index ->
            Snapshot.withMutableSnapshot { generation = index + 1 }
            host.renderUntilIdle()
        }
        val reachable = tracker.reachableGenerations(oldestExpected = OLDEST_GENERATION_A_HOST_MAY_HOLD)

        // Assert
        assertEquals(expected = REPLACEMENTS + 1, actual = tracker.madeCount)
        assertOnlyTheLatestAreReachable(reachable = reachable)
    }

    @Test
    fun `given a slot that layout replaces while measuring, when replaced often, then old ones are let go`() {
        // Arrange
        var generation by mutableStateOf(value = 0)
        val tracker = ReplacedContentTracker()
        val host = open()
        host.setContentLocked {
            SubcomposeLayout { constraints ->
                val current = generation
                val placeables = subcompose(slotId = current) {
                    TextsReadByLayersAndPlacement(content = remember { tracker.make(generation = current) })
                }.map { it.measure(constraints) }
                layout(width = constraints.maxWidth, height = constraints.maxHeight) {
                    placeables.forEach { it.place(x = 0, y = 0) }
                }
            }
        }
        host.renderUntilIdle()

        // Act
        repeat(REPLACEMENTS) { index ->
            Snapshot.withMutableSnapshot { generation = index + 1 }
            host.renderUntilIdle()
        }
        val reachable = tracker.reachableGenerations(oldestExpected = OLDEST_GENERATION_A_HOST_MAY_HOLD)

        // Assert
        assertEquals(expected = REPLACEMENTS + 1, actual = tracker.madeCount)
        assertOnlyTheLatestAreReachable(reachable = reachable)
    }

    @Test
    fun `given a state read in composition, when it changes, then the host tells with the work already pending`() {
        // Arrange
        var label by mutableStateOf(value = "before")
        val told = HostThatNotesEachTell(content = { BasicText(text = label) })

        // Act
        val pendingEachTimeTold = told.notedAfterSnapshot { label = "after" }

        // Assert
        assertEquals(expected = setOf(true), actual = pendingEachTimeTold.toSet())
    }

    @Test
    fun `given a state read in measure, when it changes, then the host tells with the work already pending`() {
        // Arrange
        var heightPx by mutableStateOf(value = 0)
        val told = HostThatNotesEachTell(
            content = {
                Layout { _, constraints -> layout(width = constraints.maxWidth, height = heightPx) {} }
            },
        )

        // Act
        val pendingEachTimeTold = told.notedAfterSnapshot { heightPx = SHIFT_PX }

        // Assert
        assertEquals(expected = setOf(true), actual = pendingEachTimeTold.toSet())
    }

    @Test
    fun `given a state read only while placing, when it changes, then the host tells with the work already pending`() {
        // Arrange
        var shift by mutableStateOf(value = 0)
        val told = HostThatNotesEachTell(
            content = {
                FixedText(text = "moving", widthDp = NAME_WIDTH_DP, modifier = Modifier.offset { IntOffset(shift, 0) })
            },
        )

        // Act
        val pendingEachTimeTold = told.notedAfterSnapshot { shift = SHIFT_PX }

        // Assert
        assertEquals(expected = setOf(true), actual = pendingEachTimeTold.toSet())
    }

    @Test
    fun `given a state read only by a layer, when it changes, then the host tells with the work already pending`() {
        // Arrange
        var shift by mutableStateOf(value = 0)
        val told = HostThatNotesEachTell(
            content = {
                FixedText(
                    text = "moving",
                    widthDp = NAME_WIDTH_DP,
                    modifier = Modifier.graphicsLayer { translationX = shift.toFloat() },
                )
            },
        )

        // Act
        val pendingEachTimeTold = told.notedAfterSnapshot { shift = SHIFT_PX }

        // Assert
        assertEquals(expected = setOf(true), actual = pendingEachTimeTold.toSet())
    }

    @Test
    fun `given a state read only in semantics, when it changes, then the host tells with the work already pending`() {
        // Arrange
        var label by mutableStateOf(value = "before")
        val told = HostThatNotesEachTell(
            content = {
                Box(
                    modifier = Modifier
                        .size(width = NAME_WIDTH_DP.dp, height = ROW_HEIGHT_DP.dp)
                        .semantics { contentDescription = label },
                )
            },
        )

        // Act
        val pendingEachTimeTold = told.notedAfterSnapshot { label = "after" }

        // Assert
        assertEquals(expected = setOf(true), actual = pendingEachTimeTold.toSet())
    }

    @Test
    fun `given a screen with no popup, when a write adds one, then the host tells and the popup shows`() {
        // Arrange
        var shown by mutableStateOf(value = false)
        val told = HostThatNotesEachTell(content = { if (shown) Popup { BasicText(text = "popup") } })

        // Act
        val pendingEachTimeTold = told.notedAfterSnapshot { shown = true }
        told.host.renderUntilIdle()

        // Assert
        assertEquals(expected = setOf(true), actual = pendingEachTimeTold.toSet())
        assertEquals(expected = listOf(listOf(), listOf("popup")), actual = told.host.textsByWindow())
    }

    @Test
    fun `given a state only another host reads, when a plain write changes it, then only that host tells`() {
        // Arrange
        val sceneThread = ManualDispatcher()
        var label by mutableStateOf(value = "before")
        val reader = HostThatNotesEachTell(coroutineContext = sceneThread) { BasicText(text = label) }
        val bystander = HostThatNotesEachTell(coroutineContext = sceneThread) { BasicText(text = "bystander") }
        runUntilIdle(sceneThread)
        val toldBefore = bystander.timesTold

        // Act
        val pendingEachTimeReaderTold = reader.notedDuring {
            label = "after"
            runUntilIdle(sceneThread)
        }

        // Assert
        assertEquals(expected = setOf(true), actual = pendingEachTimeReaderTold.toSet())
        assertEquals(expected = 0, actual = bystander.timesTold - toldBefore)
    }

    private inner class HostThatNotesEachTell(
        coroutineContext: CoroutineContext = Dispatchers.Unconfined,
        content: @Composable () -> Unit,
    ) {
        private val pendingWhenTold = mutableListOf<Boolean>()
        private var openedHost: SceneHost? = null

        val host: SceneHost = open(coroutineContext = coroutineContext, onInvalidated = { notePendingWork() })
            .also { openedHost = it }

        val timesTold: Int get() = pendingWhenTold.size

        init {
            host.setContentLocked(content = content)
            host.renderUntilIdle()
        }

        fun notedDuring(action: () -> Unit): List<Boolean> {
            val toldBefore = timesTold
            action()
            return pendingWhenTold.drop(toldBefore)
        }

        fun notedAfterSnapshot(write: () -> Unit): List<Boolean> =
            notedDuring { Snapshot.withMutableSnapshot(block = write) }

        private fun notePendingWork() {
            openedHost?.let { pendingWhenTold.add(it.hasPendingWork()) }
        }
    }

    /* A slot that layout removes stays reachable until one more has replaced it, however many came before. */
    private fun assertOnlyTheLatestAreReachable(reachable: List<Int>) {
        assertTrue(actual = REPLACEMENTS in reachable, message = "the content on screen was not reachable: $reachable")
        val older = reachable.filter { it < OLDEST_GENERATION_A_HOST_MAY_HOLD }
        assertEquals(expected = 0, actual = older.size, message = "replaced content is still reachable: $older")
    }

    private fun open(
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        coroutineContext: CoroutineContext = Dispatchers.Unconfined,
        onInvalidated: () -> Unit = {},
    ): SceneHost {
        val setup = SceneSetup(
            widthPx = WIDTH_PX,
            heightPx = HEIGHT_PX,
            density = Density(density = DENSITY),
            layoutDirection = layoutDirection,
        )
        return withSceneLock { openHost(setup, coroutineContext, onInvalidated) }.also { openHosts += it }
    }

    private fun SceneHost.setContentLocked(content: @Composable () -> Unit) {
        withSceneLock { setContent(content = content) }
    }

    private fun SceneHost.renderLocked(nanoTime: Long = FIRST_FRAME_NANOS) {
        withSceneLock { renderFrame(nanoTime = nanoTime) }
    }

    private fun SceneHost.renderUntilIdle() {
        repeat(FRAMES_TO_SETTLE) { frame ->
            renderLocked(nanoTime = FIRST_FRAME_NANOS + frame * FRAME_STEP_NANOS)
            if (!hasPendingWork()) return
        }
        fail("the host still had pending work after $FRAMES_TO_SETTLE frames")
    }

    private fun SceneHost.renderWhilePending() {
        var frames = 0
        while (hasPendingWork()) {
            if (++frames > FRAMES_TO_SETTLE) fail("the host still had pending work after $FRAMES_TO_SETTLE frames")
            renderLocked(nanoTime = FIRST_FRAME_NANOS + frames * FRAME_STEP_NANOS)
        }
    }

    private fun SceneHost.textsByWindow(): List<List<String>> = withSceneLock {
        semanticsRoots()
            .map { root -> root.selfAndDescendants().mapNotNull { it.shownText() } }
            .sortedBy { texts -> texts.joinToString() }
    }

    private fun SceneHost.closeLocked() {
        if (openHosts.remove(this)) withSceneLock { close() }
    }

    private fun SceneHost.textNodes(): List<SemanticsNode> = withSceneLock {
        semanticsRoots().flatMap { it.selfAndDescendants() }.filter { it.shownText() != null }
    }

    private fun SceneHost.placedTexts(): List<PlacedText> = withSceneLock {
        textNodes().mapNotNull { node -> node.shownText()?.let { PlacedText(text = it, bounds = node.boundsInRoot) } }
    }

    private fun SemanticsNode.selfAndDescendants(): List<SemanticsNode> =
        listOf(this) + children.flatMap { it.selfAndDescendants() }

    private fun SemanticsNode.shownText(): String? =
        config.getOrNull(SemanticsProperties.Text)?.joinToString(separator = "") { it.text }
}
