@file:JvmName("UnplaceableCallerRunner")

package app.caller

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import app.bridge.unplaceable.PaymentSection
import app.bridge.unplaceable.Receipt
import kotlin.coroutines.EmptyCoroutineContext

@Composable
private fun UnplaceableCaller() {
    PaymentSection(title = "Pay") { method ->
        Receipt(title = method)
    }
}

fun runUnplaceableCaller() {
    val composition = Composition(NoNodesApplier, Recomposer(EmptyCoroutineContext))
    composition.setContent { UnplaceableCaller() }
}

private object NoNodesApplier : AbstractApplier<Unit>(Unit) {
    override fun insertTopDown(index: Int, instance: Unit) = Unit

    override fun insertBottomUp(index: Int, instance: Unit) = Unit

    override fun remove(index: Int, count: Int) = Unit

    override fun move(from: Int, to: Int, count: Int) = Unit

    override fun onClear() = Unit
}
