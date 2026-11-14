package dev.tonholo.ssrcompose.app.data

import dev.tonholo.ssrcompose.protocol.ActionEvent
import dev.tonholo.ssrcompose.protocol.ColumnNode
import dev.tonholo.ssrcompose.protocol.FragmentNode
import dev.tonholo.ssrcompose.protocol.ReferenceNode
import dev.tonholo.ssrcompose.protocol.ScreenNode
import dev.tonholo.ssrcompose.protocol.ScreenService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart

private const val COMPLETED = "completed"

/** Writes every tree received and every action sent to [log]. Fragment bytes are logged as a size only. */
class LoggingScreenService(
    private val delegate: ScreenService,
    private val log: (String) -> Unit,
) : ScreenService {
    override suspend fun screen(name: String): ScreenNode =
        delegate.screen(name = name).also { tree -> log("screen $name <- ${tree.describe()}") }

    override fun session(name: String, events: Flow<ActionEvent>): Flow<ScreenNode> =
        delegate.session(name = name, events = events.onEach { event -> log("-> action ${event.actionId.value}") })
            .onStart { log("session $name opened") }
            .onEach { tree -> log("<- ${tree.describe()}") }
            .onCompletion { cause -> log("session $name closed: ${cause?.message ?: COMPLETED}") }
}

private fun ScreenNode.describe(): String = when (this) {
    is ColumnNode -> children.joinToString(prefix = "column[", postfix = "]") { child -> child.describe() }
    is ReferenceNode -> "$componentId$props"
    is FragmentNode -> "fragment(${document.size} bytes)"
}
