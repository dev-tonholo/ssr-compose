package dev.tonholo.ssrcompose.producer.testing

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Keeps every task until the test runs it. A dispatcher that is never run stands for a blocked thread.
 */
internal class ManualDispatcher : CoroutineDispatcher() {
    private val tasks = ArrayDeque<Runnable>()

    val hasTasks: Boolean get() = tasks.isNotEmpty()

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        tasks.addLast(block)
    }

    fun runNextTask() {
        tasks.removeFirst().run()
    }
}

internal fun runUntilIdle(vararg dispatchers: ManualDispatcher) {
    while (true) {
        val busy = dispatchers.firstOrNull { it.hasTasks } ?: return
        busy.runNextTask()
    }
}
