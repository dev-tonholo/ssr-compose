package fixtures.consumer.renderer

import androidx.compose.runtime.Composable
import dev.tonholo.ssrcompose.consumer.PlacedComponentScope
import dev.tonholo.ssrcompose.core.annotation.ConsumerComposable

@ConsumerComposable
@Composable
fun Section(content: @Composable () -> Unit) {
}

@Composable
fun CallSectionRenderer(scope: PlacedComponentScope) {
    SectionRenderer(placed = scope)
}
