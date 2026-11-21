package dev.tonholo.ssrcompose.producer.internal.scene

import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext

private const val DELAY_MILLIS = 10L

class WakingDispatcherTest {
    @Test
    fun `given work run through it, when it is handed over, then the host is told`() = runTest {
        // Arrange
        val handedOver = AtomicInteger()
        val dispatcher = WakingDispatcher(delegate = sceneDispatcher) { handedOver.incrementAndGet() }

        // Act
        withContext(dispatcher) {}

        // Assert
        assertTrue(actual = handedOver.get() > 0)
    }

    @Test
    fun `given a delay inside it, when the delay ends, then the work resumes on the thread it ran on`() = runTest {
        // Arrange
        val sceneThread = withContext(sceneDispatcher) { Thread.currentThread() }
        val dispatcher = WakingDispatcher(delegate = sceneDispatcher) {}

        // Act
        val (before, after) = withContext(dispatcher) {
            val before = Thread.currentThread()
            delay(DELAY_MILLIS)
            before to Thread.currentThread()
        }

        // Assert
        assertEquals(expected = listOf(sceneThread, sceneThread), actual = listOf(before, after))
    }
}
