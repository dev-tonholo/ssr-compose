package dev.tonholo.ssrcompose.producer.text.layoutlib

import com.android.ide.common.rendering.api.ILayoutLog

/** Keeps layoutlib's first error. layoutlib reports a failed start only here and returns false. */
internal class StartupLog : ILayoutLog {
    @Volatile
    var firstMessage: String? = null
        private set

    @Volatile
    var firstCause: Throwable? = null
        private set

    override fun error(tag: String?, message: String?, throwable: Throwable?, viewCookie: Any?, data: Any?) {
        if (firstMessage == null) {
            firstMessage = message
            firstCause = throwable
        }
    }
}
