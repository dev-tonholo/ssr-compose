package dev.tonholo.ssrcompose.producer.internal

import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext

/**
 * Tells every live screen about state writes. The whole process shares one: it watches global state writes with one
 * observer, however many screens are live.
 *
 * A write wakes one waiting screen, which applies the change for every screen's composition. A screen that leaves
 * wakes another one, because the write that woke it may have been lost on the way out. The process-wide write
 * observer exists only while at least one screen is waiting.
 *
 * @param dispatcher runs everything the waiting does: joining, waiting for writes, applying them and leaving. It is not
 * the dispatcher of any screen, so a screen that is busy, blocked or being disposed never holds back a write that
 * another screen is waiting for. It must not run work that blocks.
 * @param registerWriteObserver starts watching global state writes and returns the handle that stops it.
 */
internal class SnapshotNotifier(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val registerWriteObserver: ((Any) -> Unit) -> ObserverHandle = { observer ->
        Snapshot.registerGlobalWriteObserver(observer = observer)
    },
) {
    private val lock = Any()
    private val written = Channel<Unit>(Channel.CONFLATED)
    private var waitingScreens = 0
    private var writeObserver: ObserverHandle? = null

    /**
     * Applies state writes for every live screen until the calling coroutine is cancelled.
     */
    suspend fun applyWritesWhileWaiting(): Nothing =
        withContext(dispatcher) {
            join()
            try {
                applyWrites()
            } finally {
                leave()
            }
        }

    private suspend fun applyWrites(): Nothing {
        Snapshot.sendApplyNotifications()
        while (true) {
            written.receive()
            Snapshot.sendApplyNotifications()
        }
    }

    private fun join() = synchronized(lock) {
        waitingScreens++
        if (writeObserver == null) {
            writeObserver = registerWriteObserver { written.trySend(Unit) }
        }
    }

    private fun leave() = synchronized(lock) {
        written.trySend(Unit)
        waitingScreens--
        if (waitingScreens == 0) {
            writeObserver?.dispose()
            writeObserver = null
        }
    }

    companion object {
        val shared = SnapshotNotifier()
    }
}
