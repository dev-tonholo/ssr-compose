package dev.tonholo.ssrcompose.producer.text.layoutlib

import java.util.logging.Handler
import java.util.logging.LogRecord
import java.util.logging.Logger

/** What reached the JVM logger [LAYOUTLIB_LOGGER] while [action] ran. */
internal fun recordsWhile(action: () -> Unit): List<LogRecord> {
    val records = mutableListOf<LogRecord>()
    val recorder = object : Handler() {
        override fun publish(record: LogRecord) {
            records += record
        }

        override fun flush() = Unit

        override fun close() = Unit
    }
    val logger = Logger.getLogger(LAYOUTLIB_LOGGER)
    logger.addHandler(recorder)
    try {
        action()
    } finally {
        logger.removeHandler(recorder)
    }
    return records
}
