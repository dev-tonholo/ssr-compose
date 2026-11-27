package dev.tonholo.ssrcompose.producer.text.layoutlib

import java.nio.file.Path
import java.util.logging.Level
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

private val RUNTIME_ROOT = Path.of("layoutlib-runtime")

private const val TAG = "startup"
private const val WARNING = "a warning before any error"
private const val FIRST_ERROR = "the first error"
private const val SECOND_ERROR = "a later error"

class JvmLayoutLogTest {
    @Test
    fun `given two errors after a warning, when reported, then the first error is kept and all reach the JVM log`() {
        // Arrange
        val log = JvmLayoutLog()
        val cause = IllegalStateException("why it failed")

        // Act
        val logged = recordsWhile {
            log.warning(tag = TAG, message = WARNING, viewCookie = null, data = null)
            log.error(tag = TAG, message = FIRST_ERROR, throwable = cause, viewCookie = null, data = null)
            log.error(tag = TAG, message = SECOND_ERROR, viewCookie = null, data = null)
        }

        // Assert
        assertEquals(expected = FIRST_ERROR, actual = log.firstErrorMessage)
        assertSame(expected = cause, actual = log.firstErrorCause)
        assertEquals(
            expected = listOf(Level.WARNING, Level.SEVERE, Level.SEVERE),
            actual = logged.map { it.level },
        )
    }

    @Test
    fun `given a first error with no message, when another follows, then the start failure reports the first cause`() {
        // Arrange
        val log = JvmLayoutLog()
        val firstCause = IllegalStateException("why it failed")
        val laterCause = IllegalStateException("a consequence")

        // Act
        log.error(tag = TAG, message = null, throwable = firstCause, viewCookie = null, data = null)
        log.error(tag = TAG, message = SECOND_ERROR, throwable = laterCause, viewCookie = null, data = null)
        val failure = LayoutlibBridge.startFailure(root = RUNTIME_ROOT, log = log)

        // Assert
        assertSame(expected = firstCause, actual = failure.cause)
        assertNull(log.firstErrorMessage)
        assertEquals(
            expected = "layoutlib did not start from $RUNTIME_ROOT: layoutlib reported an error with no message",
            actual = failure.message,
        )
    }

    @Test
    fun `given no error reported, when a start fails, then the failure says layoutlib does not retry`() {
        // Arrange
        val log = JvmLayoutLog()

        // Act
        val failure = LayoutlibBridge.startFailure(root = RUNTIME_ROOT, log = log)

        // Assert
        assertNull(failure.cause)
        assertEquals(
            expected = "layoutlib did not start from $RUNTIME_ROOT: " +
                "an earlier start failed in this process, and layoutlib does not retry",
            actual = failure.message,
        )
    }
}
