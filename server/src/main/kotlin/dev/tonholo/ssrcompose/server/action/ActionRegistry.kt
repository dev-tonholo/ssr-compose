package dev.tonholo.ssrcompose.server.action

import dev.tonholo.ssrcompose.protocol.ActionId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/** Holds server lambdas that a client can call by [ActionId]. Safe to use from many coroutines. */
class ActionRegistry {
    private val actions = ConcurrentHashMap<ActionId, () -> Unit>()
    private val counter = AtomicLong()

    fun register(action: () -> Unit): ActionId {
        val id = ActionId("action-${counter.incrementAndGet()}")
        actions[id] = action
        return id
    }

    fun unregister(id: ActionId) {
        actions.remove(id)
    }

    /** Runs the action for [id]. Returns false, and does nothing else, when the id is unknown. */
    fun dispatch(id: ActionId): Boolean {
        val action = actions[id] ?: return false
        action()
        return true
    }
}
