package dev.tonholo.ssrcompose.producer.memory

import androidx.compose.runtime.mutableStateOf
import dev.tonholo.ssrcompose.producer.testing.ShownText

private const val SETTLE_PAUSE_MS = 3000L

/**
 * Opens one more live session, changes a state only that session reads, and returns how many frames [sessions] render
 * because of the change.
 */
internal fun framesForAnotherChange(sessions: MemorySessions): Long {
    val onlyReadThere = mutableStateOf(value = "before")
    return liveSessions { ShownText(text = onlyReadThere.value) }.use { other ->
        other.open(count = 1) { true }
        Thread.sleep(SETTLE_PAUSE_MS)
        val before = checkNotNull(sessions.framesRendered)
        onlyReadThere.value = "after"
        Thread.sleep(SETTLE_PAUSE_MS)
        checkNotNull(sessions.framesRendered) - before
    }
}
