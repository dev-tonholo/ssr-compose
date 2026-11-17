package fixtures.consumer

import androidx.compose.runtime.Composable
import dev.tonholo.ssrcompose.core.annotation.ConsumerComposable
import dev.tonholo.ssrcompose.core.annotation.ProducerComposable

@ProducerComposable
typealias InnerProducerAlias = @Composable () -> Unit

typealias OuterProducerAlias = InnerProducerAlias

@ConsumerComposable
@Composable
fun NestedTypealiasSlotContainer(
    content: OuterProducerAlias,
) {
}
