package dev.tonholo.ssrcompose.producer.text.layoutlib

import com.android.ide.common.rendering.api.ILayoutLog
import java.util.logging.Level
import java.util.logging.Logger

internal const val LAYOUTLIB_LOGGER = "dev.tonholo.ssrcompose.producer.text.layoutlib"

private const val LAYOUTLIB_WARNING_PRIORITY = 3
private const val LAYOUTLIB_ERROR_PRIORITY = 4

/**
 * Writes what layoutlib and the Android code in it report to the JVM logger [LAYOUTLIB_LOGGER], and keeps the first
 * error layoutlib itself reports. layoutlib says why a start failed only there, and returns false.
 */
internal class JvmLayoutLog : ILayoutLog {
    private val logger = Logger.getLogger(LAYOUTLIB_LOGGER)

    @Volatile
    var hasRecordedError = false
        private set

    @Volatile
    var firstErrorMessage: String? = null
        private set

    @Volatile
    var firstErrorCause: Throwable? = null
        private set

    override fun error(tag: String?, message: String?, viewCookie: Any?, data: Any?) {
        error(tag = tag, message = message, throwable = null, viewCookie = viewCookie, data = data)
    }

    override fun error(tag: String?, message: String?, throwable: Throwable?, viewCookie: Any?, data: Any?) {
        if (!hasRecordedError) {
            hasRecordedError = true
            firstErrorMessage = message
            firstErrorCause = throwable
        }
        write(level = Level.SEVERE, tag = tag, message = message, cause = throwable)
    }

    override fun warning(tag: String?, message: String?, viewCookie: Any?, data: Any?) {
        write(level = Level.WARNING, tag = tag, message = message)
    }

    override fun fidelityWarning(
        tag: String?,
        message: String?,
        throwable: Throwable?,
        viewCookie: Any?,
        data: Any?,
    ) {
        write(level = Level.WARNING, tag = tag, message = message, cause = throwable)
    }

    override fun logAndroidFramework(priority: Int, tag: String?, message: String?) {
        val level = when {
            priority >= LAYOUTLIB_ERROR_PRIORITY -> Level.SEVERE
            priority == LAYOUTLIB_WARNING_PRIORITY -> Level.WARNING
            else -> Level.INFO
        }
        write(level = level, tag = tag, message = message)
    }

    private fun write(level: Level, tag: String?, message: String?, cause: Throwable? = null) {
        logger.log(level, "layoutlib $tag: $message", cause)
    }
}
