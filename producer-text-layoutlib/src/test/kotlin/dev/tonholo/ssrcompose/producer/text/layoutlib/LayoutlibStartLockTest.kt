package dev.tonholo.ssrcompose.producer.text.layoutlib

import com.android.layoutlib.bridge.Bridge
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val START_TIMEOUT_SECONDS = 30L

class LayoutlibStartLockTest {
    @Test
    fun `given the render lock held by another thread, when layoutlib first starts, then the start completes`() {
        // Arrange
        check(Bridge.getLog() !is JvmLayoutLog) {
            "layoutlib already started in this JVM; this test needs the test task's forkEvery = 1"
        }
        val otherThread = Executors.newSingleThreadExecutor { task -> Thread(task).apply { isDaemon = true } }
        val renderLock = Bridge.getLock()
        renderLock.lock()

        // Act
        val start = try {
            runCatching {
                otherThread.submit(Callable { testEngine() }).get(START_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            }
        } finally {
            renderLock.unlock()
            otherThread.shutdown()
        }

        // Assert
        assertNull(start.exceptionOrNull())
        assertTrue(actual = Bridge.getLog() is JvmLayoutLog, message = "the start did not install its log")
    }
}
