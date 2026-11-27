package dev.tonholo.ssrcompose.producer.internal.compat

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import dev.tonholo.ssrcompose.producer.internal.scene.SceneSetup
import dev.tonholo.ssrcompose.producer.internal.scene.withSceneLock
import dev.tonholo.ssrcompose.producer.testing.ManualDispatcher
import dev.tonholo.ssrcompose.producer.testing.runUntilIdle
import kotlin.coroutines.CoroutineContext
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
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
private fun FixedText(text: String, widthDp: Int) {
    BasicText(text = text, modifier = Modifier.size(width = widthDp.dp, height = ROW_HEIGHT_DP.dp))
}

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
