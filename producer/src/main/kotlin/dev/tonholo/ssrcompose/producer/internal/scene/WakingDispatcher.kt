package dev.tonholo.ssrcompose.producer.internal.scene

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable

/**
 * Runs everything on [delegate], the way [delegate] does, and calls [onDispatch] each time something is handed to it.
 * A scene has no callback for new work, so its host gives the scene this as its dispatcher and learns that a delay
 * ended, an effect resumed or a state change reached the recomposer.
 *
 * @param onDispatch must not block, since it runs on the thread that hands the work over.
 */
internal class WakingDispatcher(
    private val delegate: CoroutineDispatcher,
    private val onDispatch: () -> Unit,
) : CoroutineDispatcher() {
    override fun isDispatchNeeded(context: CoroutineContext): Boolean = delegate.isDispatchNeeded(context)

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        delegate.dispatch(context, block)
        onDispatch()
    }
}
