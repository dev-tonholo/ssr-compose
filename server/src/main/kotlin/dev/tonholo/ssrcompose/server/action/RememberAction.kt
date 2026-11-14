package dev.tonholo.ssrcompose.server.action

import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import dev.tonholo.ssrcompose.protocol.ActionId

val LocalActionRegistry = compositionLocalOf<ActionRegistry> {
    error("No ActionRegistry provided. Wrap the content in CompositionLocalProvider(LocalActionRegistry provides ...).")
}

/**
 * Registers [action] so a client can call it by the returned id.
 * The id stays the same across recompositions, the latest lambda always runs,
 * and the action is unregistered when it leaves the composition.
 */
@Composable
fun rememberAction(action: () -> Unit): ActionId {
    val registry = LocalActionRegistry.current
    val latestAction = rememberUpdatedState(action)
    return remember(registry) { RegisteredAction(registry, latestAction) }.id
}

private class RegisteredAction(
    private val registry: ActionRegistry,
    latestAction: State<() -> Unit>,
) : RememberObserver {
    val id: ActionId = registry.register { latestAction.value() }

    override fun onRemembered() = Unit

    override fun onForgotten() = registry.unregister(id)

    override fun onAbandoned() = registry.unregister(id)
}
