package dev.tonholo.ssrcompose.consumer.testing

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Composes [content] once, without a UI, and disposes it. An exception from [content] reaches the caller.
 */
fun composeOnce(content: @Composable () -> Unit) {
    val recomposer = Recomposer(effectCoroutineContext = EmptyCoroutineContext)
    val composition = Composition(applier = NoopApplier(), parent = recomposer)
    try {
        composition.setContent(content = content)
    } finally {
        composition.dispose()
        recomposer.cancel()
    }
}

internal class NoopApplier : AbstractApplier<Unit>(root = Unit) {
    override fun insertTopDown(index: Int, instance: Unit) = Unit

    override fun insertBottomUp(index: Int, instance: Unit) = Unit

    override fun remove(index: Int, count: Int) = Unit

    override fun move(from: Int, to: Int, count: Int) = Unit

    override fun onClear() = Unit
}
