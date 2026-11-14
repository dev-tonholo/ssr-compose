package dev.tonholo.ssrcompose.server.tree

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.ObserverHandle
import androidx.compose.runtime.snapshots.Snapshot
import dev.tonholo.ssrcompose.protocol.ScreenNode
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Runs [content] as a live composition for each collector and emits the tree after every applied change.
 * The composition is disposed when the collector cancels.
 */
fun liveTree(content: @Composable () -> Unit): Flow<ScreenNode> =
    channelFlow { runLiveComposition(content) }
        .buffer(Channel.CONFLATED)
        .distinctUntilChanged()

private suspend fun ProducerScope<ScreenNode>.runLiveComposition(content: @Composable () -> Unit) {
    val root = MutableNode.Column()
    val recomposer = Recomposer(coroutineContext)
    val applier = TreeApplier(root = root, onChangesApplied = { trySend(root.toScreenNode()) })
    val composition = Composition(applier = applier, parent = recomposer)
    val writeObserver = sendApplyNotificationsOnGlobalWrites()
    launch(ImmediateFrameClock) { recomposer.runRecomposeAndApplyChanges() }
    composition.setContent(content)
    trySend(root.toScreenNode())
    awaitClose {
        writeObserver.dispose()
        composition.dispose()
        recomposer.cancel()
    }
}

private fun ProducerScope<*>.sendApplyNotificationsOnGlobalWrites(): ObserverHandle {
    val notificationScheduled = AtomicBoolean(false)
    return Snapshot.registerGlobalWriteObserver {
        if (notificationScheduled.compareAndSet(false, true)) {
            launch {
                notificationScheduled.set(false)
                Snapshot.sendApplyNotifications()
            }
        }
    }
}

private object ImmediateFrameClock : MonotonicFrameClock {
    override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R = onFrame(System.nanoTime())
}
