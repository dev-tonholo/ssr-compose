package dev.tonholo.ssrcompose.server

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import dev.tonholo.ssrcompose.protocol.ActionEvent
import dev.tonholo.ssrcompose.protocol.ScreenNode
import dev.tonholo.ssrcompose.protocol.ScreenService
import dev.tonholo.ssrcompose.server.action.ActionRegistry
import dev.tonholo.ssrcompose.server.action.LocalActionRegistry
import dev.tonholo.ssrcompose.server.tree.liveTree
import dev.tonholo.ssrcompose.server.tree.renderTree
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ScreenServiceImpl(private val screens: Map<String, @Composable () -> Unit>) : ScreenService {
    override suspend fun screen(name: String): ScreenNode {
        val content = screenNamed(name)
        return renderTree { CompositionLocalProvider(LocalActionRegistry provides ActionRegistry()) { content() } }
    }

    override fun session(name: String, events: Flow<ActionEvent>): Flow<ScreenNode> = channelFlow {
        val content = screenNamed(name)
        val registry = ActionRegistry()
        launch { events.catch { }.collect { event -> registry.dispatch(event.actionId) } }
        liveTree { CompositionLocalProvider(LocalActionRegistry provides registry) { content() } }
            .collect { send(it) }
    }

    private fun screenNamed(name: String): @Composable () -> Unit =
        requireNotNull(screens[name]) { "Unknown screen: $name" }
}
