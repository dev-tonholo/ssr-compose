package fixtures.consumer

import androidx.compose.runtime.Composable
import dev.tonholo.ssrcompose.core.annotation.ConsumerComposable
import dev.tonholo.ssrcompose.core.annotation.ProducerComposable

@ProducerComposable
typealias InnerConflictingAlias = @Composable () -> Unit

typealias OuterConflictingAlias = @ConsumerComposable InnerConflictingAlias

@ConsumerComposable
@Composable
fun NestedConflictingSlotContainer(
    content: OuterConflictingAlias,
) {
}
