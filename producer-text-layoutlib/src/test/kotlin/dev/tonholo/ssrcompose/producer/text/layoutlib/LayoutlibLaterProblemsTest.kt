package dev.tonholo.ssrcompose.producer.text.layoutlib

import android.util.Log
import com.android.layoutlib.bridge.Bridge
import java.util.logging.Level
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val TAG = "later-problem"
private const val ANDROID_ERROR = "an error from Android code"
private const val LAYOUTLIB_ERROR = "an error from layoutlib"
private const val CAUSED_ERROR = "an error with a cause"
private const val ANDROID_ASSERT = "an assert from Android code"
private const val ANDROID_WARNING = "a warning from Android code"
private const val ANDROID_NOTE = "a note from Android code"
private val ERRORS = listOf(ANDROID_ERROR, ANDROID_ASSERT, LAYOUTLIB_ERROR, CAUSED_ERROR)

class LayoutlibLaterProblemsTest {
    @Test
    fun `given a started engine, when Android code or layoutlib reports an error, then the JVM log has it`() {
        // Arrange
        testEngine()
        val cause = IllegalStateException("the cause")

        // Act
        val logged = recordsWhile {
            Log.e(TAG, ANDROID_ERROR)
            Log.println(Log.ASSERT, TAG, ANDROID_ASSERT)
            Bridge.getLog().error(TAG, LAYOUTLIB_ERROR, null, null)
            Bridge.getLog().error(TAG, CAUSED_ERROR, cause, null, null)
        }

        // Assert
        val errors = logged.filter { it.level == Level.SEVERE }
        val found = ERRORS.filter { text -> errors.any { text in it.message } }
        assertEquals(expected = ERRORS, actual = found)
        assertTrue(errors.any { it.thrown === cause })
    }

    @Test
    fun `given a started engine, when Android code warns or notes, then the JVM log has each at its own level`() {
        // Arrange
        testEngine()

        // Act
        val logged = recordsWhile {
            Log.w(TAG, ANDROID_WARNING)
            Log.i(TAG, ANDROID_NOTE)
        }

        // Assert
        val levels = listOf(ANDROID_WARNING, ANDROID_NOTE).map { text -> logged.find { text in it.message }?.level }
        assertEquals(expected = listOf(Level.WARNING, Level.INFO), actual = levels)
    }
}
