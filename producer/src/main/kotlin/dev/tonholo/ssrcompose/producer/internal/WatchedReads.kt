package dev.tonholo.ssrcompose.producer.internal

import androidx.compose.runtime.snapshots.SnapshotStateObserver

/**
 * Watches the states that the last [read] of one screen read. Call [read] and [close] on one thread.
 *
 * @param onChanged called when one of those states changes, after [haveChanged] says so. It runs on the thread that
 * applies the change, which holds the observer's lock, so it must not block or touch the scene.
 */
internal class WatchedReads(private val onChanged: () -> Unit) : AutoCloseable {
    /** Whether a state the last [read] read has changed since it started. */
    @Volatile
    var haveChanged: Boolean = false
        private set

    private val observer = SnapshotStateObserver(onChangedExecutor = { notify -> notify() }).apply { start() }

    private val noteChange: (Unit) -> Unit = {
        haveChanged = true
        onChanged()
    }

    /** Runs [block] and watches the states it reads, in place of those of the read before. */
    fun <T : Any> read(block: () -> T): T {
        observer.clear()
        haveChanged = false
        var result: T? = null
        observer.observeReads(scope = Unit, onValueChangedForScope = noteChange) { result = block() }
        return checkNotNull(result)
    }

    override fun close() {
        observer.stop()
        observer.clear()
    }
}
