package dev.tonholo.ssrcompose.producer.scene

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.InspectableValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.tonholo.ssrcompose.producer.testing.DEFAULT_SCENE_WIDTH_PX
import dev.tonholo.ssrcompose.producer.testing.SceneSpec
import dev.tonholo.ssrcompose.producer.testing.openTestScene
import dev.tonholo.ssrcompose.producer.testing.rootChildNodes
import dev.tonholo.ssrcompose.producer.testing.withRenderedScene
import kotlin.test.Test
import kotlin.test.assertEquals
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield

private const val DENSITY = 2.5f
private const val FONT_SCALE = 1.3f
private const val OFFSET_X_DP = 16
private const val OFFSET_Y_DP = 8
private const val BOX_WIDTH_DP = 100
private const val BOX_HEIGHT_DP = 50
private const val SP_AS_DP_SOURCE = 10
private const val SIBLING_SIZE_DP = 10
private const val INNER_PADDING_DP = 4
private const val BACKGROUND_MODIFIER = "background"
private const val FLOAT_TOLERANCE = 0.01f
private const val SESSION_THREAD_NAME = "ssr-session"
private const val EXPECTED_THREAD_RECORDS = 4

class SceneEnvironmentTest {
    @Test
    fun `given a density, when a box has padding after its background, then the background's box divides to dp`() {
        // Arrange
        val spec = SceneSpec(density = Density(density = DENSITY, fontScale = FONT_SCALE))

        // Act
        val box = withRenderedScene(
            spec = spec,
            content = {
                Box(
                    Modifier
                        .offset(x = OFFSET_X_DP.dp, y = OFFSET_Y_DP.dp)
                        .size(width = BOX_WIDTH_DP.dp, height = BOX_HEIGHT_DP.dp)
                        .background(Color.Red)
                        .padding(INNER_PADDING_DP.dp),
                )
            },
            read = { scene -> scene.rootChildNodes().single().backgroundBoxInDp(density = DENSITY) },
        )

        // Assert
        assertEquals(expected = OFFSET_X_DP.toFloat(), actual = box.x, absoluteTolerance = FLOAT_TOLERANCE)
        assertEquals(expected = OFFSET_Y_DP.toFloat(), actual = box.y, absoluteTolerance = FLOAT_TOLERANCE)
        assertEquals(expected = BOX_WIDTH_DP.toFloat(), actual = box.width, absoluteTolerance = FLOAT_TOLERANCE)
        assertEquals(expected = BOX_HEIGHT_DP.toFloat(), actual = box.height, absoluteTolerance = FLOAT_TOLERANCE)
    }

    @Test
    fun `given a font scale, when a size in sp is turned into dp, then it is scaled by the font scale`() {
        // Arrange
        val spec = SceneSpec(density = Density(density = DENSITY, fontScale = FONT_SCALE))
        val expectedDp = SP_AS_DP_SOURCE * FONT_SCALE
        var actualDp = 0f

        // Act
        withRenderedScene(
            spec = spec,
            content = {
                val density = LocalDensity.current
                actualDp = with(density) { SP_AS_DP_SOURCE.sp.toDp().value }
            },
            read = {},
        )

        // Assert
        assertEquals(expected = expectedDp, actual = actualDp, absoluteTolerance = FLOAT_TOLERANCE)
    }

    @Test
    fun `given each direction, when two boxes sit in a row in the root box, then the second follows the first`() {
        // Arrange
        val rtlRowStart = DEFAULT_SCENE_WIDTH_PX - 2f * SIBLING_SIZE_DP
        val expected = mapOf(LayoutDirection.Ltr to SIBLING_SIZE_DP.toFloat(), LayoutDirection.Rtl to rtlRowStart)

        // Act
        val actual = LayoutDirection.entries.associateWith { direction -> secondBoxX(direction) }

        // Assert
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `given a scene driven from the session thread, when it runs effects and recomposes, then all runs on it`() {
        // Arrange
        val executor = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, SESSION_THREAD_NAME) }
        val dispatcher = executor.asCoroutineDispatcher()
        val threads = ConcurrentLinkedQueue<String>()
        val effectDone = CompletableDeferred<Unit>()
        val state = mutableStateOf(value = 0)
        val content: @Composable () -> Unit = {
            state.value
            threads.add(currentThreadName())
            LaunchedEffect(Unit) {
                threads.add(currentThreadName())
                yield()
                threads.add(currentThreadName())
                effectDone.complete(Unit)
            }
        }

        // Act
        try {
            runOnSession(executor) {
                openTestScene(spec = SceneSpec(dispatcher = dispatcher), content = content).use { session ->
                    session.render()
                    runBlocking { effectDone.await() }
                    state.value += 1
                    session.render()
                }
            }
        } finally {
            executor.shutdown()
        }

        // Assert
        assertEquals(expected = setOf(SESSION_THREAD_NAME), actual = threads.toSet())
        assertEquals(expected = EXPECTED_THREAD_RECORDS, actual = threads.size)
    }

    private fun currentThreadName(): String = Thread.currentThread().name.substringBefore(' ')

    private fun runOnSession(executor: ExecutorService, block: () -> Unit) {
        executor.submit(block).get()
    }

    private fun secondBoxX(direction: LayoutDirection): Float = withRenderedScene(
        spec = SceneSpec(layoutDirection = direction),
        content = {
            Row {
                Box(Modifier.size(SIBLING_SIZE_DP.dp).background(Color.Red))
                Box(Modifier.size(SIBLING_SIZE_DP.dp).background(Color.Blue))
            }
        },
        read = { scene -> scene.rootChildNodes().last().backgroundBoxInDp(density = 1f).x },
    )

    private fun SemanticsNode.backgroundBoxInDp(density: Float): BoxInDp {
        val background = layoutInfo.getModifierInfo().first { info ->
            (info.modifier as? InspectableValue)?.nameFallback == BACKGROUND_MODIFIER
        }
        val position = background.coordinates.positionInRoot()
        val size = background.coordinates.size
        return BoxInDp(
            x = position.x / density,
            y = position.y / density,
            width = size.width / density,
            height = size.height / density,
        )
    }

    private data class BoxInDp(val x: Float, val y: Float, val width: Float, val height: Float)
}
