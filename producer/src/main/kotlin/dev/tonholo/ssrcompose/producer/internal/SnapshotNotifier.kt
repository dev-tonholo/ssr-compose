package dev.tonholo.ssrcompose.producer.internal

import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import kotlinx.coroutines.channels.Channel

/**
 * Tells every live screen about state writes. The whole process shares one: it watches global state writes with one
 * observer, however many screens are live.
 *
 * A write wakes one waiting screen, which applies the change for all of them. The process-wide write observer exists
 * only while at least one screen is waiting.
 */
internal class SnapshotNotifier {
    private val lock = Any()
    private val written = Channel<Unit>(Channel.CONFLATED)
    private var waitingScreens = 0
    private var writeObserver: ObserverHandle? = null

    /**
     * Applies state writes for every live screen until the calling coroutine is cancelled.
     */
    suspend fun applyWritesWhileWaiting(): Nothing {
        join()
        try {
            Snapshot.sendApplyNotifications()
            while (true) {
                written.receive()
                Snapshot.sendApplyNotifications()
            }
        } finally {
            leave()
        }
    }

    private fun join() = synchronized(lock) {
        waitingScreens++
        if (writeObserver == null) {
            writeObserver = Snapshot.registerGlobalWriteObserver { written.trySend(Unit) }
        }
    }

    private fun leave() = synchronized(lock) {
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
