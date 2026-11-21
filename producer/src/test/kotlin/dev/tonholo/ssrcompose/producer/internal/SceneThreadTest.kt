package dev.tonholo.ssrcompose.producer.internal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.tonholo.ssrcompose.producer.testing.ShownText
import dev.tonholo.ssrcompose.producer.testing.liveSetup
import dev.tonholo.ssrcompose.producer.testing.shown
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

/**
 * R20 "Scenes and threads", invariants 12 and 13, ADR 0021: every scene step runs on the producer's one scene thread,
 * and a composition that blocks holds back no other thread. The tests wait on latches; the timeouts only stop a
 * test that would otherwise hang.
 */
class SceneThreadTest {
    @Test
    fun `given 100 idle scene thread screens, when all states change from many threads, then each shows its change`() =
        withGuard {
            // Arrange
            val states = List(SCREEN_COUNT) { mutableStateOf(FIRST_TEXT) }
            val shown = List(SCREEN_COUNT) { MutableStateFlow<List<String>?>(null) }
            val expected = List(SCREEN_COUNT) { index -> listOf(changedText(index)) }
            val collectors = states.mapIndexed { index, state -> collectShown(state = state, into = shown[index]) }
            shown.forEach { screen -> screen.first { it != null } }

            // Act
            states.mapIndexed { index, state -> launch { state.value = changedText(index) } }.joinAll()
            val actual = shown.mapIndexed { index, screen -> screen.first { it == expected[index] } }
            collectors.forEach { it.cancelAndJoin() }

            // Assert
            assertEquals(expected = expected, actual = actual)
        }

    @Test
    fun `given two screens, when they run effects, recompose, are read and closed, then all ran on the scene thread`() =
        withGuard {
            // Arrange
            val threads = ConcurrentHashMap<String, MutableSet<Thread>>()
            val effectsDone = List(SCREEN_PAIR) { CompletableDeferred<Unit>() }
            val texts = List(SCREEN_PAIR) { mutableStateOf(FIRST_TEXT) }
            val screens = List(SCREEN_PAIR) { index ->
                LiveScreen(
                    content = {
                        RecordingScreen(text = texts[index].value, threads = threads, done = effectsDone[index])
                    },
                    setup = liveSetup(),
                )
            }

            // Act
            val collectors = screens.mapIndexed { index, screen ->
                launch {
                    var frames = 0
                    screen.frames().collect {
                        frames++
                        if (frames == 1) {
                            effectsDone[index].await()
                            texts[index].value = SECOND_TEXT
                        }
                    }
                }
            }
            effectsDone.joinAll()
            threads.waitFor(step = RECOMPOSED)
            collectors.forEach { it.cancelAndJoin() }
            threads.waitFor(step = CLOSED)

            // Assert
            val usedThreads = threads.values.flatten().toSet()
            assertEquals(expected = RECORDED_STEPS.toSet(), actual = threads.keys)
            assertEquals(expected = 1, actual = usedThreads.size)
            assertEquals(expected = EXPECTED_SCENE_THREAD_NAME, actual = usedThreads.single().name.substringBefore(' '))
        }

    @Test
    fun `given an effect blocked off the scene thread, when another screen changes twice, then both changes show`() =
        withGuard {
            // Arrange
            val release = CountDownLatch(1)
            val effectBlocked = CompletableDeferred<Unit>()
            val watched = mutableStateOf(FIRST_TEXT)
            val watchedShown = MutableStateFlow<List<String>?>(null)
            val blocking = launch {
                LiveScreen(
                    content = {
                        ShownText(text = FIRST_TEXT)
                        LaunchedEffect(Unit) {
                            withContext(Dispatchers.IO) {
                                effectBlocked.complete(Unit)
                                release.await()
                            }
                        }
                    },
                    setup = liveSetup(),
                ).frames().collect {}
            }
            val watching = collectShown(state = watched, into = watchedShown)
            try {
                effectBlocked.await()
                watchedShown.first { it != null }

                // Act
                watched.value = SECOND_TEXT
                val afterFirst = watchedShown.first { it == listOf(SECOND_TEXT) }
                watched.value = THIRD_TEXT
                val afterSecond = watchedShown.first { it == listOf(THIRD_TEXT) }

                // Assert
                val expected = listOf(listOf(SECOND_TEXT), listOf(THIRD_TEXT))
                assertEquals(expected = expected, actual = listOf(afterFirst, afterSecond))
            } finally {
                release.countDown()
                listOf(blocking, watching).forEach { it.cancelAndJoin() }
            }
        }

    @Test
    fun `given a blocked composition, when twice as many screens as cores change, then a Default coroutine runs`() =
        withGuard {
            // Arrange
            val release = CountDownLatch(1)
            val blocked = CompletableDeferred<Unit>()
            val blocking = mutableStateOf(FIRST_TEXT)
            val screenCount = 2 * Runtime.getRuntime().availableProcessors()
            val states = List(screenCount) { mutableStateOf(FIRST_TEXT) }
            val shown = List(screenCount) { MutableStateFlow<List<String>?>(null) }
            val blockingCollector = launch {
                blockingScreen(state = blocking, blocked = blocked, release = release).frames().collect {}
            }
            val collectors = states.mapIndexed { index, state -> collectShown(state = state, into = shown[index]) }
            try {
                shown.forEach { screen -> screen.first { it != null } }
                blocking.value = SECOND_TEXT
                blocked.await()

                // Act
                states.mapIndexed { index, state -> launch { state.value = changedText(index) } }.joinAll()
                val ranOnDefault = async(Dispatchers.Default) { true }.await()

                // Assert
                assertTrue(actual = ranOnDefault)
                assertEquals(expected = 1L, actual = release.count)
            } finally {
                release.countDown()
                (collectors + blockingCollector).forEach { it.cancelAndJoin() }
            }
        }

    @Test
    fun `given a blocked composition, when it is released, then the other screen shows its latest change`() =
        withGuard {
            // Arrange
            val release = CountDownLatch(1)
            val blocked = CompletableDeferred<Unit>()
            val blocking = mutableStateOf(FIRST_TEXT)
            val watched = mutableStateOf(FIRST_TEXT)
            val watchedShown = MutableStateFlow<List<String>?>(null)
            val blockingCollector = launch {
                blockingScreen(state = blocking, blocked = blocked, release = release).frames().collect {}
            }
            val watching = collectShown(state = watched, into = watchedShown)
            try {
                watchedShown.first { it != null }
                blocking.value = SECOND_TEXT
                blocked.await()
                watched.value = SECOND_TEXT
                watched.value = THIRD_TEXT

                // Act
                release.countDown()
                val actual = watchedShown.first { it == listOf(THIRD_TEXT) }

                // Assert
                assertEquals(expected = listOf(THIRD_TEXT), actual = actual)
            } finally {
                release.countDown()
                listOf(blockingCollector, watching).forEach { it.cancelAndJoin() }
            }
        }

    @Composable
    private fun RecordingScreen(
        text: String,
        threads: MutableMap<String, MutableSet<Thread>>,
        done: CompletableDeferred<Unit>,
    ) {
        threads.record(step = if (text == FIRST_TEXT) COMPOSED else RECOMPOSED)
        Box(
            Modifier
                .semantics { threads.record(step = READ) }
                .layout { measurable, constraints ->
                    threads.record(step = MEASURED)
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) { placeable.place(x = 0, y = 0) }
                }
                .size(BOX_SIZE_DP.dp),
        )
        ShownText(text = text)
        LaunchedEffect(Unit) {
            threads.record(step = EFFECT)
            yield()
            threads.record(step = EFFECT)
            done.complete(Unit)
        }
        DisposableEffect(Unit) { onDispose { threads.record(step = CLOSED) } }
    }

    private fun blockingScreen(
        state: MutableState<String>,
        blocked: CompletableDeferred<Unit>,
        release: CountDownLatch,
    ) =
        LiveScreen(
            content = {
                ShownText(text = state.value)
                if (state.value == SECOND_TEXT) {
                    blocked.complete(Unit)
                    release.await()
                }
            },
            setup = liveSetup(),
        )

    private fun CoroutineScope.collectShown(state: MutableState<String>, into: MutableStateFlow<List<String>?>): Job =
        launch {
            LiveScreen(content = { ShownText(text = state.value) }, setup = liveSetup())
                .frames()
                .collect { into.value = it.shown().texts }
        }

    private fun MutableMap<String, MutableSet<Thread>>.record(step: String) {
        computeIfAbsent(step) { ConcurrentHashMap.newKeySet() }.add(Thread.currentThread())
    }

    private suspend fun Map<String, Set<Thread>>.waitFor(step: String) {
        while (step !in this) yield()
    }

    private fun withGuard(block: suspend CoroutineScope.() -> Unit) {
        val test = FutureTask { runBlocking(Dispatchers.Default) { block() } }
        Thread(test, GUARDED_THREAD_NAME).apply { isDaemon = true }.start()
        try {
            test.get(GUARD_TIMEOUT.inWholeSeconds, TimeUnit.SECONDS)
        } catch (failure: ExecutionException) {
            throw failure.cause ?: failure
        } finally {
            test.cancel(true)
        }
    }

    private fun changedText(index: Int) = "$SECOND_TEXT $index"

    private companion object {
        const val FIRST_TEXT = "Welcome"
        const val SECOND_TEXT = "Updated"
        const val THIRD_TEXT = "Updated again"
        const val SCREEN_COUNT = 100
        const val SCREEN_PAIR = 2
        const val BOX_SIZE_DP = 10
        const val COMPOSED = "composed"
        const val RECOMPOSED = "recomposed"
        const val MEASURED = "measured"
        const val READ = "read"
        const val EFFECT = "effect"
        const val CLOSED = "closed"
        val RECORDED_STEPS = listOf(COMPOSED, RECOMPOSED, MEASURED, READ, EFFECT, CLOSED)
        const val EXPECTED_SCENE_THREAD_NAME = "ssr-compose-scene"
        const val GUARDED_THREAD_NAME = "scene-thread-test"
        val GUARD_TIMEOUT = 60.seconds
    }
}
