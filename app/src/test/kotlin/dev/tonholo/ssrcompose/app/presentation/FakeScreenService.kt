package dev.tonholo.ssrcompose.app.presentation

import dev.tonholo.ssrcompose.protocol.ActionEvent
import dev.tonholo.ssrcompose.protocol.ActionId
import dev.tonholo.ssrcompose.protocol.ScreenNode
import dev.tonholo.ssrcompose.protocol.ScreenService
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

class FakeScreenService : ScreenService {
    private val trees = mutableListOf<Channel<ScreenNode>>()
    private val actions = mutableListOf<ActionId>()

    val sessionCount: Int get() = trees.size
    val receivedActions: List<ActionId> get() = actions.toList()

    fun emit(node: ScreenNode) {
        trees.last().trySend(node)
    }

    fun fail(cause: Throwable) {
        trees.last().close(cause)
    }

    fun complete() {
        trees.last().close()
    }

    override suspend fun screen(name: String): ScreenNode = error("The app follows sessions only")

    override fun session(name: String, events: Flow<ActionEvent>): Flow<ScreenNode> {
        val session = Channel<ScreenNode>(Channel.UNLIMITED)
        trees += session
        return channelFlow {
            launch { events.collect { event -> actions += event.actionId } }
            for (node in session) send(node)
        }
    }
}
