package fixtures.consumer.renderer

import androidx.compose.runtime.Composable
import dev.tonholo.ssrcompose.consumer.PlacedComponentScope
import dev.tonholo.ssrcompose.core.annotation.ConsumerComposable

@ConsumerComposable
@Composable
fun Counter(count: Int) {
}

@Composable
fun CallCounterRenderer(scope: PlacedComponentScope) {
    CounterRenderer(placed = scope)
}
