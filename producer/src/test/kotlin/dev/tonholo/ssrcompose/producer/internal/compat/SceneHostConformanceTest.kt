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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import dev.tonholo.ssrcompose.producer.internal.scene.SceneSetup
import dev.tonholo.ssrcompose.producer.internal.scene.withSceneLock
import dev.tonholo.ssrcompose.producer.testing.ManualDispatcher
import dev.tonholo.ssrcompose.producer.testing.runUntilIdle
import kotlin.coroutines.CoroutineContext
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
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

private class StateMadeInLayout {
    var shift: MutableState<Int>? = null

    fun readOrMake(): Int = (shift ?: mutableStateOf(value = 0).also { shift = it }).value
}

@Composable
private fun PlacedBy(content: @Composable () -> Unit, place: Placeable.PlacementScope.(Placeable) -> Unit) {
    Layout(content = content) { measurables, constraints ->
        val placeable = measurables.single().measure(constraints)
        layout(width = constraints.maxWidth, height = placeable.height) { place(placeable) }
    }
}

private fun nameAt(left: Float, top: Float = 0f, height: Float = ROW_HEIGHT_PX, text: String = "moving"): PlacedText =
    PlacedText(text = text, bounds = Rect(left = left, top = top, right = left + NAME_WIDTH_PX, bottom = top + height))

/**
 * What every [SceneHost] must do. An implementation's test class extends this one with the function that opens it.
 */
internal abstract class SceneHostConformanceTest(
    private val openHost: (SceneSetup, CoroutineContext) -> SceneHost,
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

    private fun open(
        layoutDirection: LayoutDirection = LayoutDirection.Ltr,
        coroutineContext: CoroutineContext = Dispatchers.Unconfined,
    ): SceneHost {
        val setup = SceneSetup(
            widthPx = WIDTH_PX,
            heightPx = HEIGHT_PX,
            density = Density(density = DENSITY),
            layoutDirection = layoutDirection,
        )
        return withSceneLock { openHost(setup, coroutineContext) }.also { openHosts += it }
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
